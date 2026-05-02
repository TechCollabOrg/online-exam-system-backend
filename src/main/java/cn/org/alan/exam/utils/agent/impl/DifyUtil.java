package cn.org.alan.exam.utils.agent.impl;

import cn.hutool.json.JSONObject;
import cn.org.alan.exam.utils.agent.AIChat;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * {@link AIChat} 的 Dify 实现：对配置的 {@code dify.base-url} 发起阻塞模式（{@code response_mode=blocking}）对话请求，
 * 从响应 JSON 读取 {@code answer}。请求体中的 {@code inputs} 需按实际应用工作流调整。
 * <p>
 * 配置 {@code online-exam.chat-platform.type=dify} 时生效。
 *
 * @author 赵浩森
 * @since 2025/4/17
 */
@Service
@ConditionalOnProperty(name = "online-exam.chat-platform.type", havingValue = "dify")
public class DifyUtil implements AIChat {

    @Value("${dify.api-key}")
    private String apiKey;

    /** 对话 API 完整 URL（一般为应用或 chat-messages 端点，依 Dify 部署而定）。 */
    @Value("${dify.base-url}")
    private String baseUrl;

    /**
     * 调用 Dify 阻塞对话接口，解析响应 JSON 中的 {@code answer} 字段。
     */
    @Override
    public String getChatResponse(String msg) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(baseUrl);

            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            JSONObject requestBody = new JSONObject();
            requestBody.put("query", msg);
            requestBody.put("response_mode", "blocking");
            requestBody.put("user", "user");

            JSONObject inputs = new JSONObject();
            inputs.put("text", "Your input text");
            requestBody.put("inputs", inputs);

            StringEntity entity = new StringEntity(requestBody.toString());
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseString = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    return new JSONObject(responseString).getStr("answer");
                } else {
                    throw new RuntimeException("HTTP " + statusCode + " Error: " + responseString);
                }
            }
        }
    }
}