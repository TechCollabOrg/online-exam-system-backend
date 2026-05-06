package cn.org.alan.exam.common.handler;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 控制器层统一异常出口：将各类异常转为 {@link Result#failed(String)}，避免堆栈直接暴露给前端。
 * <p>
 * 业务异常使用异常消息原文；校验类异常提取首条错误文案；数据库唯一约束等映射为简短中文提示。
 * </p>
 *
 * @author WeiJin
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务主动抛出的运行时异常（{@link ServiceRuntimeException}），通常携带可读的业务提示文案。
     *
     * @param e 业务异常
     * @return {@code code=0} 的失败响应，{@code msg} 为异常消息
     */
    @ExceptionHandler(ServiceRuntimeException.class)
    public Result<String> handleException(ServiceRuntimeException e) {
        String message = e.getMessage();
        log.error("接口调用异常: {}", message);
        return Result.failed(message);
    }

    /**
     * {@code @Valid} 校验失败（如表单 DTO 字段约束），取绑定结果中第一条 {@code defaultMessage} 返回。
     *
     * @param e Spring MVC 方法参数校验异常
     * @return 失败响应，消息为首条校验错误说明
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e.getClass());
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.failed(message);
    }

    /**
     * JDBC 层违反 SQL 完整性约束（如唯一索引），统一返回固定文案「重复」，避免暴露数据库细节。
     *
     * @param e SQL 完整性约束异常
     * @return 失败响应
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<String> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        log.error(e.getMessage(), e.getClass());
        return Result.failed("重复");
    }

    /**
     * 请求体 JSON/XML 反序列化失败或格式非法（如类型不匹配、缺少必需字段），返回「请求参数无法解析」。
     *
     * @param e HTTP 消息不可读异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e.getClass());
        return Result.failed("请求参数无法解析");
    }

    /**
     * 必填的 Query/Form 参数未传递，返回「参数名为必填项」形式的提示。
     *
     * @param e 缺少 Servlet 请求参数异常
     * @return 失败响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error(e.getMessage(), e.getClass());
        return Result.failed(e.getParameterName() + "为必填项");
    }

    /**
     * MyBatis-Plus 捕获的数据库重复键（插入/更新冲突）；从异常消息中截取冲突字段名拼接「主键冲突…已存在」。
     * <p>注意：依赖异常消息格式，若驱动文案变化可能需调整解析逻辑。</p>
     *
     * @param e 重复键异常
     * @return 失败响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicateKeyException(DuplicateKeyException e) {
        String name = e.getMessage().split(":")[2].split(" ")[3];
        log.error(e.getMessage(), e.getClass());
        return Result.failed("主键冲突" + name + "已存在");
    }

    /**
     * 方法安全等抛出的访问拒绝（如 {@code @PreAuthorize} 不满足）：与 {@code SecurityConfig} 中过滤器链的
     * {@code accessDeniedHandler} 一致，返回 HTTP 403 + 统一 JSON，便于 axios 按状态码区分「未登录」与「无权限」。
     *
     * @param e 访问被拒绝异常
     * @return HTTP 403 与 {@link Result#failed(String)} 体
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<String>> handleAccessDeniedException(AccessDeniedException e) {
        log.error(e.getMessage(), e.getClass());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.failed("你没有该资源的访问权限"));
    }


    /**
     * 上传文件超过 Spring 配置的单次/总大小限制，提示最大 5MB（需与 {@code multipart} 配置保持一致）。
     *
     * @param e 超过最大上传大小异常
     * @return 失败响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handlerMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error(e.getMessage(), e.getClass());
        return Result.failed("文件太大，最大上传5MB");
    }

    /**
     * Multipart 请求中缺少某个 {@code part}（常见于前端未按约定字段名上传文件）。
     *
     * @param e 缺少请求部件异常
     * @return 失败响应
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<String> handlerMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.error(e.getMessage(), e.getClass());
        return Result.failed("没有获取到文件");
    }


    /**
     * 方法级 {@code @Validated}（如单个路径变量）触发的 Bean Validation 约束违反，直接返回异常消息字符串。
     *
     * @param e 约束违反异常
     * @return 失败响应，{@code msg} 通常含属性路径与约束说明
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolationException(ConstraintViolationException e) {
        log.error(e.getMessage(), e.getClass());
        return Result.failed(e.getMessage());
    }
}
