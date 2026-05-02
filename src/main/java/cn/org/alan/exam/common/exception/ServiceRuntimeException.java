package cn.org.alan.exam.common.exception;

/**
 * 业务层运行时异常：携带可读错误信息，由全局异常处理器转换为统一响应。
 *
 * @author Alan
 * @since 2025/3/20
 */
public class ServiceRuntimeException extends RuntimeException {
    /**
     * @param msg 展示给用户或写入日志的业务错误描述
     */
    public ServiceRuntimeException(String msg) {
        super(msg);
    }
}
