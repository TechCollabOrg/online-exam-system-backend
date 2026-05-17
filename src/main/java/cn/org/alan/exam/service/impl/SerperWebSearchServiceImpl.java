package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.config.AiGradingProperties;
import cn.org.alan.exam.service.IAiGradingWebSearchService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 通过 Serper（Google 搜索 API）获取联网参考资料；{@code serper.api-key} 由环境变量注入。
 */
@Slf4j
@Service
public class SerperWebSearchServiceImpl implements IAiGradingWebSearchService {

    private final AiGradingProperties aiGradingProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${serper.api-key:}")
    private String serperApiKey;

    @Value("${serper.api-url:https://google.serper.dev/search}")
    private String serperApiUrl;

    public SerperWebSearchServiceImpl(AiGradingProperties aiGradingProperties) {
        this.aiGradingProperties = aiGradingProperties;
    }

    @Override
    public String searchReference(String questionPlainText) {
        if (!aiGradingProperties.isWebSearchEnabled() || StringUtils.isBlank(serperApiKey)) {
            return "";
        }
        if (StringUtils.isBlank(questionPlainText)) {
            return "";
        }
        String query = questionPlainText.length() > 120
                ? questionPlainText.substring(0, 120)
                : questionPlainText;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", serperApiKey);

            JSONObject body = new JSONObject();
            body.put("q", query);
            body.put("num", aiGradingProperties.getWebSearchMaxResults());

            HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(serperApiUrl, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Serper 检索失败: status={}", response.getStatusCode());
                return "";
            }
            return formatOrganicSnippets(response.getBody());
        } catch (Exception e) {
            log.warn("Serper 联网检索异常: {}", e.getMessage());
            return "";
        }
    }

    private String formatOrganicSnippets(String jsonBody) {
        JSONObject root = JSONObject.parseObject(jsonBody);
        JSONArray organic = root.getJSONArray("organic");
        if (organic == null || organic.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(organic.size(), aiGradingProperties.getWebSearchMaxResults());
        for (int i = 0; i < limit; i++) {
            JSONObject item = organic.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String title = item.getString("title");
            String snippet = item.getString("snippet");
            if (StringUtils.isBlank(snippet)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(i + 1).append(". ");
            if (StringUtils.isNotBlank(title)) {
                sb.append(title).append("：");
            }
            sb.append(snippet.trim());
        }
        return sb.toString();
    }
}
