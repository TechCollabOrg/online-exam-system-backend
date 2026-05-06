package cn.org.alan.exam.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
// import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Auth0 JWT（HS256）的令牌签发与校验：载荷含序列化用户信息字符串与权限列表；
 * 支持在剩余有效期低于 {@code jwt.refreshThreshold} 时自动续签为新 Token。
 *
 * @author WeiJin
 */
@Component
@Slf4j
@SuppressWarnings("all")
public class JwtUtil {

    /** 对称签名密钥（配置文件 {@code jwt.secret}）。 */
    @Value("${jwt.secret}")
    private String secret;

    /** 令牌有效期（毫秒），自签发时刻起算。 */
    @Value("${jwt.expiration}")
    private long expiration;

    /** 剩余寿命低于该毫秒数时 {@link #verifyAndRefreshToken(String)} 会签发新 Token。 */
    @Value("${jwt.refreshThreshold}")
    private long refreshThreshold;

    /**
     * 签发 JWT：自定义声明 {@code userInfo}（一般为 JSON 字符串）、{@code authList}（角色/权限名列表）。
     *
     * @param userInfo 用户信息载荷
     * @param authList Spring Security 可用的权限字符串列表
     * @return 带 Bearer 前缀外置于 Header 的原始 JWT 串
     */
    public String createJwt(String userInfo, List<String> authList) {
        Date issDate = new Date();// 签发时间
        Date expireDate = new Date(issDate.getTime() + expiration);
        // 定义头部信息
        Map<String, Object> headerClaims = new HashMap<>();
        headerClaims.put("alg", "HS256"); // 算法
        headerClaims.put("typ", "JWT"); // 类型只能是jwt
        List<String> safeAuthList = authList != null ? authList : Collections.emptyList();
        return JWT.create().withHeader(headerClaims)
                .withIssuer("wj") // 签发人
                .withIssuedAt(issDate) // 签发时间
                .withExpiresAt(expireDate) // 过期时间
                .withClaim("userInfo", userInfo) // 自定义声明
                .withClaim("authList", safeAuthList)
                .sign(Algorithm.HMAC256(secret)); // 使用HS256作为签名，SECRET作为密钥
    }

    /**
     * 获取 JWT 验证器实例。
     *
     * @return 返回一个 JWTVerifier 实例，用于验证 JWT 的有效性。
     */
    private JWTVerifier getVerifier() {
        // 使用 JWT 库的 require 方法，指定使用 HMAC256 算法进行签名验证
        // HMAC256 是一种对称加密算法，使用相同的密钥进行签名和验证
        // secret 是用于生成和验证 JWT 签名的密钥，需要确保其安全性
        // build 方法用于构建最终的 JWTVerifier 实例
        return JWT.require(Algorithm.HMAC256(secret)).build();
    }
    /**
     * 校验token并尝试续签
     *
     * @param token token令牌
     * @return 若不需要续签返回原 Token，若需要续签返回新 Token，若验证失败返回 null
     */
    public String verifyAndRefreshToken(String token) {
        JWTVerifier verifier = getVerifier();
        try {
            DecodedJWT jwt = verifier.verify(token);
            // 检查是否需要续签
            if (shouldRefresh(jwt)) {
                String userInfo = jwt.getClaim("userInfo").asString();
                List<String> authList = jwt.getClaim("authList").asList(String.class);
                if (authList == null) {
                    authList = Collections.emptyList();
                }
                return createJwt(userInfo, authList);
            }
            return token;
        } catch (JWTVerificationException e) {
            log.error("校验失败", e);
            return null;
        }
    }

    /**
     * 若距离过期时间不足 {@link #refreshThreshold} 则返回 true，触发重新签发。
     *
     * @param jwt 已解码的令牌
     * @return 是否需要续签
     */
    private boolean shouldRefresh(DecodedJWT jwt) {
        Date expirationDate = jwt.getExpiresAt();
        long currentTime = System.currentTimeMillis();
        long remainingTime = expirationDate.getTime() - currentTime;
        return remainingTime < refreshThreshold;
    }

    /**
     * 仅校验签名与有效期，不做续签。
     *
     * @param token JWT 字符串
     * @return 合法为 true，否则 false
     */
    public boolean verifyToken(String token) {
        // 构建jwt校验器
        JWTVerifier verifier = getVerifier();
        try {

            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.error("校验失败");
            return false;
        }
    }

    /**
     * 校验通过后读取声明 {@code userInfo}（调用方负责反序列化为业务对象）。
     *
     * @param token JWT
     * @return 用户信息 JSON 字符串；校验失败返回 {@code null}
     */
    public String getUser(String token) {
        // 构建jwt校验器
        JWTVerifier verifier = getVerifier();
        try {
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaim("userInfo").asString();
        } catch (JWTVerificationException e) {
            log.error("用户获取失败");
            return null;
        }
    }

    /**
     * 校验通过后读取声明 {@code authList}。
     *
     * @param token JWT
     * @return 权限字符串列表；校验失败返回 {@code null}
     */
    public List<String> getAuthList(String token) {
        // 构建jwt校验器
        JWTVerifier verifier = getVerifier();
        try {
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaim("authList").asList(String.class);
        } catch (JWTVerificationException e) {
            log.error("权限列表+获取失败");
            return null;
        }
    }

}
