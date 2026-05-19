package cn.org.alan.exam.utils.agent;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * OpenAI 兼容接口：规范化 Base URL、拉取模型列表、测试连通性。
 */
public final class LlmConnectionHelper {

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    private LlmConnectionHelper() {
    }

    public static String normalizeBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new ServiceRuntimeException("请填写 API 基础地址（需包含 /v1，例如 https://api.siliconflow.cn/v1）");
        }
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * GET {baseUrl}/models
     */
    public static List<String> fetchModelIds(String baseUrl, String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            throw new ServiceRuntimeException("请填写 API 密钥");
        }
        String normalized = normalizeBaseUrl(baseUrl);
        String url = normalized + "/models";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey.trim());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.GET, entity, String.class);
            return parseModelIds(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw new ServiceRuntimeException("拉取模型列表失败：" + extractErrorMessage(e));
        } catch (Exception e) {
            throw new ServiceRuntimeException("拉取模型列表失败：" + e.getMessage());
        }
    }

    public static void testConnection(String baseUrl, String apiKey) {
        fetchModelIds(baseUrl, apiKey);
    }

    private static List<String> parseModelIds(String body) {
        if (StringUtils.isBlank(body)) {
            return Collections.emptyList();
        }
        JSONObject root = JSONObject.parseObject(body);
        JSONArray data = root.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        TreeSet<String> sorted = new TreeSet<>();
        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (item != null && StringUtils.isNotBlank(item.getString("id"))) {
                sorted.add(item.getString("id"));
            }
        }
        return new ArrayList<>(sorted);
    }

    private static String extractErrorMessage(HttpStatusCodeException e) {
        String body = e.getResponseBodyAsString();
        if (StringUtils.isNotBlank(body)) {
            try {
                JSONObject json = JSONObject.parseObject(body);
                JSONObject err = json.getJSONObject("error");
                if (err != null && StringUtils.isNotBlank(err.getString("message"))) {
                    return err.getString("message");
                }
            } catch (Exception ignored) {
                // ignore parse error
            }
            if (body.length() > 200) {
                return body.substring(0, 200);
            }
            return body;
        }
        return e.getStatusCode() + " " + e.getStatusText();
    }
}
