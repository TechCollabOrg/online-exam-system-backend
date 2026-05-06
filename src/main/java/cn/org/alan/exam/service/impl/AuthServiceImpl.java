package cn.org.alan.exam.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.converter.UserConverter;
import cn.org.alan.exam.mapper.RoleMapper;
import cn.org.alan.exam.mapper.UserDailyLoginDurationMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.model.entity.Log;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.entity.UserDailyLoginDuration;
import cn.org.alan.exam.model.form.auth.LoginForm;
import cn.org.alan.exam.model.form.auth.VerifyCodeForm;
import cn.org.alan.exam.model.form.user.UserForm;
import cn.org.alan.exam.model.vo.auth.CaptchaVO;
import cn.org.alan.exam.service.IAuthService;
import cn.org.alan.exam.service.ILogService;
import cn.org.alan.exam.utils.*;
import cn.org.alan.exam.utils.security.SysUserDetails;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 认证注册登录：验证码图片、用户名密码校验、JWT + Redis 会话、BCrypt 密码、游客注册；
 * 含心跳/在线时长写入 Redis、登录日志落库等辅助逻辑。
 * <p>
 * 《需求分析文档》映射说明：
 * </p>
 * <ul>
 *   <li>STU-01 / 身份认证矩阵：学号用户名登录、密码加盐存储（BCrypt）、令牌无状态认证；「记住我」由前端延长 Cookie 配合实现。</li>
 *   <li>5.2：登录成功签发 JWT，过期与刷新阈值见 {@code application-*.yml} 中 {@code jwt.*}。</li>
 *   <li>ADM-07：登录成功写 {@link ILogService}，便于审计（敏感操作另在业务层记录）。</li>
 * </ul>
 *
 * @author Alan
 */
@Slf4j
@Service
public class AuthServiceImpl implements IAuthService {
    private static final String HEARTBEAT_KEY_PREFIX = "user:heartbeat:";
    private static final long HEARTBEAT_INTERVAL_MILLIS = 10 * 60 * 1000; // 10分钟
    /** 图形验证码答案在 Redis 中的前缀（与 Session 解耦，避免 img 请求与 axios 会话不一致） */
    private static final String CAPTCHA_CODE_KEY_PREFIX = "captcha:code:";
    /**
     * 校验通过后写入 Redis，登录/注册请求携带同一 captchaId 即可，无需依赖 Cookie Session。
     * Electron 使用 file:// 打开页面时，跨域 Session 与浏览器不一致，仅用 Session 会误报「请先验证验证码」。
     */
    private static final String LOGIN_CAPTCHA_OK_PREFIX = "loginCaptchaOk:";
    /**
     * 图形验证码画布与干扰线数量。干扰线原配置为 300，绘制与 JPEG 编码耗时高，首屏拉取 Base64 明显变慢；
     * 适度缩小画布并降低干扰线，在可读性与机器识别难度之间折中。
     */
    private static final int CAPTCHA_WIDTH = 140;
    private static final int CAPTCHA_HEIGHT = 44;
    private static final int CAPTCHA_CODE_COUNT = 4;
    private static final int CAPTCHA_INTERFERE_COUNT = 28;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserConverter userConverter;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private UserDailyLoginDurationMapper userDailyLoginDurationMapper;
    @Value("${online-exam.login.captcha.enabled}")
    private boolean captchaEnabled;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    private ILogService logService;

    /**
     * 用户名密码登录：可选图形验证码前置校验；密码经客户端 AES 解密后与 BCrypt 比对；
     * 签发 JWT、写入 Redis（sessionId→token）、填充 SecurityContext，并记录登录日志。
     */
    @SneakyThrows(JsonProcessingException.class)
    @Override
    public Result<String> login(HttpServletRequest request, LoginForm loginForm) {
        // 图形验证码：必须通过 verifyCode，且在 Redis 中为本次 captchaId 记下「已通过」（与 Session 解耦，兼容 Electron file://）
        if (captchaEnabled) {
            if (StringUtils.isBlank(loginForm.getCaptchaId())) {
                throw new ServiceRuntimeException("请先验证验证码");
            }
            String okKey = LOGIN_CAPTCHA_OK_PREFIX + loginForm.getCaptchaId();
            if (!"1".equals(stringRedisTemplate.opsForValue().get(okKey))) {
                throw new ServiceRuntimeException("请先验证验证码");
            }
            stringRedisTemplate.delete(okKey);
        }
        // 根据用户名获取用户信息
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserName, loginForm.getUsername());
        User user = userMapper.selectOne(wrapper);
        // 判读用户名是否存在
        if (Objects.isNull(user)) {
            throw new ServiceRuntimeException("该用户不存在");
        }
        if (user.getIsDeleted() == 1) {
            throw new ServiceRuntimeException("该用户已注销");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String userPassword = SecretUtils.desEncrypt(loginForm.getPassword());
        if (!encoder.matches(userPassword, user.getPassword())) {
            throw new ServiceRuntimeException("密码错误");
        }
        user.setPassword(null);
        // 根据用户角色代码
        List<String> permissions = roleMapper.selectCodeById(user.getRoleId());

        // 数据库获取的权限是字符串springSecurity需要实现GrantedAuthority接口类型，所有这里做一个类型转换
        List<SimpleGrantedAuthority> userPermissions = permissions.stream()
                .map(permission -> new SimpleGrantedAuthority("role_" + permission)).collect(java.util.stream.Collectors.toList());

        // 创建一个sysUserDetails对象，该类实现了UserDetails接口
        SysUserDetails sysUserDetails = new SysUserDetails(user);
        // 把转型后的权限放进sysUserDetails对象
        sysUserDetails.setPermissions(userPermissions);
        // 将用户序列化 转为字符串
        String userInfo = objectMapper.writeValueAsString(user);
        // 创建token
        String token = jwtUtil.createJwt(userInfo, userPermissions.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList()));
        // 把token放到redis中
        stringRedisTemplate.opsForValue().set("token:" + request.getSession().getId(), token, 30, TimeUnit.MINUTES);

        // 封装用户的身份信息，为后续的身份验证和授权操作提供必要的输入
        // 创建UsernamePasswordAuthenticationToken  参数：用户信息，密码，权限列表
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(sysUserDetails, user.getPassword(), userPermissions);

        // 可选，添加Web认证细节
        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 用户信息存放进上下文
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        // 记录日志
        String device = httpServletRequest.getHeader("User-Agent");
        String ipRegion = Optional.ofNullable(IPUtils.getIPRegion(httpServletRequest)).orElse("暂无信息");
        Log log = Log.builder()
                .place(ipRegion)
                .device(extractDeviceType(device))
                .behavior("设备登录")
                .userId(user.getId()).build();
        logService.add(log);
        return Result.success("登录成功", token);
    }


    /**
     * 从 HTTP {@code User-Agent} 头括号段截取简要设备/平台片段，用于日志展示。
     */
    public static String extractDeviceType(String userAgent) {
        // 定义正则表达式模式
        String pattern = "\\((.*?);";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(userAgent);
        if (m.find()) {
            // 返回匹配到的设备类型
            return m.group(1);
        }
        return null;
    }

    /**
     * 登出：剥离 Bearer Token、删除 Redis 中会话 token、失效 HttpSession，并写登出日志。
     */
    @Override
    public Result<String> logout(HttpServletRequest request) {
        // 清除session
        HttpSession session = request.getSession(false);
        String token = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(token) && session != null) {
            // 记录日志：JWT 未通过过滤器落到 SysUserDetails 时 principal 可能为匿名 String，不可强转
            Integer userIdForLog = resolveUserIdForLogout(token);
            String device = httpServletRequest.getHeader("User-Agent");
            String ipRegion = Optional.ofNullable(IPUtils.getIPRegion(httpServletRequest)).orElse("暂无信息");
            Log logEntry = Log.builder()
                    .place(ipRegion)
                    .device(extractDeviceType(device))
                    .behavior("设备登出")
                    .userId(userIdForLog).build();
            logService.add(logEntry);
            token = stripBearerPrefix(token);
            stringRedisTemplate.delete("token:" + request.getSession().getId());
            session.invalidate();
        }
        return Result.success("退出成功");
    }

    /**
     * 登出写日志用的用户 ID：优先 {@link SysUserDetails}，否则从 Authorization JWT 解析。
     */
    private Integer resolveUserIdForLogout(String authorizationHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SysUserDetails) {
            return ((SysUserDetails) authentication.getPrincipal()).getUser().getId();
        }
        try {
            String raw = stripBearerPrefix(authorizationHeader);
            String userInfo = jwtUtil.getUser(raw);
            if (StringUtils.isNotBlank(userInfo)) {
                User u = objectMapper.readValue(userInfo, User.class);
                return u.getId();
            }
        } catch (Exception e) {
            log.warn("logout: parse userId from token failed: {}", e.getMessage());
        }
        return null;
    }

    /** 创建线段干扰型图形验证码（参数见类内 {@code CAPTCHA_*} 常量）。 */
    private static LineCaptcha newLineCaptcha() {
        return CaptchaUtil.createLineCaptcha(
                CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_CODE_COUNT, CAPTCHA_INTERFERE_COUNT);
    }

    /** 去掉 Authorization 上的 Bearer 前缀（忽略大小写）；无前缀则返回 trim 后的原串。 */
    private static String stripBearerPrefix(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        final int len = "Bearer ".length();
        if (v.length() >= len && v.regionMatches(true, 0, "Bearer ", 0, len)) {
            return v.substring(len).trim();
        }
        return v;
    }

    /**
     * 生成 Hutool 线段验证码图片写入响应流；正确答案存入 Redis（key 含 sessionId，短期有效）。
     */
    @SneakyThrows(IOException.class)
    @Override
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        LineCaptcha captcha = newLineCaptcha();

        // 把验证码存放进redis
        // 获取验证码
        String code = captcha.getCode();
        String key = "code" + request.getSession().getId();
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        // 把图片响应到输出流
        // Hutool AbstractCaptcha 使用 PNG 编码写出，须与 Content-Type 一致，否则浏览器可能无法正确显示
        response.setContentType("image/png");
        ServletOutputStream os = response.getOutputStream();
        captcha.write(os);
        os.close();
    }

    /**
     * 生成图形码并写入 Redis（键为 captchaId）；图片以 Base64 返回，前端用 axios 拉取以保证与校验、登录共用会话。
     */
    @Override
    public Result<CaptchaVO> getCaptchaJson(HttpServletRequest request) {
        // 验证码答案仅依赖 captchaId + Redis，无需强制创建 HttpSession，可减少冷启动首包延迟
        LineCaptcha captcha = newLineCaptcha();
        String code = captcha.getCode();
        String captchaId = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(CAPTCHA_CODE_KEY_PREFIX + captchaId, code, 10, TimeUnit.MINUTES);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        captcha.write(baos);
        String imageBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setImageBase64(imageBase64);
        return Result.success("ok", vo);
    }

    /**
     * 校验图形验证码：通过后删除验证码键并写入 {@code isVerifyCode+sessionId}，供登录/注册阶段校验。
     */
    @Override
    public Result<String> verifyCode(HttpServletRequest request, VerifyCodeForm form) {
        if (form == null || StringUtils.isBlank(form.getCode())) {
            throw new ServiceRuntimeException("请输入验证码");
        }
        if (StringUtils.isBlank(form.getCaptchaId())) {
            throw new ServiceRuntimeException("请重新获取验证码");
        }
        String code = form.getCode().trim();
        String key = CAPTCHA_CODE_KEY_PREFIX + form.getCaptchaId();
        String rightCode = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(rightCode)) {
            throw new ServiceRuntimeException("验证码已过期");
        }
        if (!rightCode.equalsIgnoreCase(code)) {
            throw new ServiceRuntimeException("验证码错误");
        }
        // 验证码校验后redis清除验证码，避免重复使用
        stringRedisTemplate.delete(key);
        // 同一 captchaId 标记已通过校验（登录/注册请求 Body 携带 captchaId；不依赖 Session Cookie）
        stringRedisTemplate.opsForValue().set(LOGIN_CAPTCHA_OK_PREFIX + form.getCaptchaId(), "1", 5, TimeUnit.MINUTES);
        return Result.success("验证码校验成功");
    }

    /**
     * 学生注册：须先通过验证码校验；两次密码一致后 BCrypt 入库，默认角色为学生（{@code roleId=1}）。
     */
    @Override
    public Result<String> register(HttpServletRequest request, UserForm userForm) {
        if (captchaEnabled) {
            if (StringUtils.isBlank(userForm.getCaptchaId())) {
                throw new ServiceRuntimeException("请先验证验证码");
            }
            String regOkKey = LOGIN_CAPTCHA_OK_PREFIX + userForm.getCaptchaId();
            if (!"1".equals(stringRedisTemplate.opsForValue().get(regOkKey))) {
                throw new ServiceRuntimeException("请先验证验证码");
            }
            stringRedisTemplate.delete(regOkKey);
        }
        // 判断两次密码是否一致
        if (!SecretUtils.desEncrypt(userForm.getPassword()).equals(SecretUtils.desEncrypt(userForm.getCheckedPassword()))) {
            throw new ServiceRuntimeException("两次密码不一致");
        }
        User user = userConverter.fromToEntity(userForm);
        user.setPassword(new BCryptPasswordEncoder().encode(SecretUtils.desEncrypt(user.getPassword())));
        user.setRoleId(1);
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    /**
     * 学生心跳：维护 Redis 上次活跃时间，用于统计在线时长并写入 {@link UserDailyLoginDuration}（间隔超过阈值累加）。
     */
    @Override
    public Result<String> sendHeartbeat(HttpServletRequest request) {
        // 创建Redis键
        String key = HEARTBEAT_KEY_PREFIX + SecurityUtil.getUserId();
        if (SecurityUtil.getRoleCode() == 1) {
            // 删除该键值并获取上一次的心跳时间
            String lastHeartbeatStr = stringRedisTemplate.opsForValue().get(key);
            // 获取当前时间
            LocalDateTime utcTime = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime now = utcTime.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
            // 设置新的时间
            stringRedisTemplate.opsForValue().set(key, now.toString());
            LocalDateTime lastHeartbeat = null;
            // 将上次时间字符串转换为时间对象
            if (lastHeartbeatStr == null) {
                lastHeartbeat = now;
            } else {
                lastHeartbeat = LocalDateTime.parse(lastHeartbeatStr);
            }
            // 计算上次和现在过了多久 连个时间的时间差
            Duration durationSinceLastHeartbeat = Duration.between(lastHeartbeat, now);
            // 获取今天日期
            LocalDate date = DateTimeUtil.getDate();
            // 实现累加逻辑，比如更新数据库中的记录
            // 获取当前用户的今天的记录
            Integer userId = SecurityUtil.getUserId();
            UserDailyLoginDuration userDailyLogin = userDailyLoginDurationMapper.getTodayRecord(userId, date);
            // 如果记录为空
            if (Objects.isNull(userDailyLogin)) {
                // 如果没记录
                UserDailyLoginDuration userDailyLoginDuration = new UserDailyLoginDuration();
                // 设置用户id
                userDailyLoginDuration.setUserId(userId);
                // 设置今天日期
                userDailyLoginDuration.setLoginDate(date);
                // 存入秒数
                userDailyLoginDuration.setTotalSeconds((int) durationSinceLastHeartbeat.getSeconds());
                userDailyLoginDurationMapper.insert(userDailyLoginDuration);
            } else {
                // 如果有记录
                // 累加今天的时长
                userDailyLogin.setTotalSeconds(userDailyLogin.getTotalSeconds()
                        + (int) durationSinceLastHeartbeat.getSeconds());
                userDailyLoginDurationMapper.updateById(userDailyLogin);
            }
        }
        return Result.success("请求成功");
    }
}
