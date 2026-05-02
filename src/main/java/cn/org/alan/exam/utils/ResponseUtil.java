package cn.org.alan.exam.utils;

import cn.org.alan.exam.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
// import jakarta.annotation.Resource;
// import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * 将 {@link Result} 序列化为 JSON 并写入 {@link HttpServletResponse}，用于过滤器或异常处理中无法通过 Controller 返回体的场景。
 *
 * @author WeiJin
 */
@Component
public class ResponseUtil {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 以 HTTP 200 写入 JSON 响应体（UTF-8，{@code application/json}）。
     *
     * @param response Servlet 响应对象
     * @param result   统一业务包装结果
     */
    @SneakyThrows({JsonProcessingException.class, IOException.class})
    public void response(HttpServletResponse response, Result result) {
        String s = objectMapper.writeValueAsString(result);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.write(s);
        writer.flush();
        writer.close();
    }

    /**
     * 写入 JSON 响应体并指定 HTTP 状态码（如 401、403），便于与安全过滤器配合。
     *
     * @param response Servlet 响应对象
     * @param result   统一业务包装结果
     * @param status   HTTP 状态码
     */
    @SneakyThrows({JsonProcessingException.class, IOException.class})
    public void response(HttpServletResponse response, Result result, Integer status) {
        String s = objectMapper.writeValueAsString(result);
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.write(s);
        writer.flush();
        writer.close();
    }
}
