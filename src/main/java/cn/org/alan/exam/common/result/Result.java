package cn.org.alan.exam.common.result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 前后端统一的 JSON 包装：{@code code=1} 表示成功，{@code code=0} 表示失败（见 {@link #failed(String)}），{@code data} 承载载荷。
 *
 * @author haoxr
 */
@Data
@ApiModel("标准响应结构体")
public class Result<T> implements Serializable {

    @ApiModelProperty("状态码")
    private Integer code;

    @ApiModelProperty("响应数据")
    private T data;

    @ApiModelProperty("响应消息")
    private String msg;

    /**
     * 无消息、无数据的成功响应（{@code code=1}）。
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应，同时设置提示文案与业务数据。
     *
     * @param msg  提示信息
     * @param data 业务数据
     */
    public static <T> Result<T> success(String msg,T data) {
        Result<T> result = new Result<>();
        result.setCode(1);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 成功响应，仅设置提示文案，{@code data} 为 {@code null}。
     *
     * @param msg 提示信息
     */
    public static <T> Result<T> success(String msg) {
        Result<T> result = new Result<>();
        result.setCode(1);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    /**
     * 失败响应（{@code code=0}），无业务数据。
     *
     * @param msg 错误说明（将展示给前端）
     */
    public static <T> Result<T> failed(String msg) {
        return result(0,msg , null);
    }

    /**
     * 内部构造：写入状态码、消息与载荷。
     */
    private static <T> Result<T> result(Integer code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setData(data);
        result.setMsg(msg);
        return result;
    }
}
