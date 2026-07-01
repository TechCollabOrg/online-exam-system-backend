package cn.org.alan.exam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LiveKit 自托管监考服务配置（见 application-dev.yml livekit.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "livekit")
public class LiveKitProperties {

    private boolean enabled = true;

    /** WebSocket 地址，如 ws://127.0.0.1:7880 */
    private String url = "ws://127.0.0.1:7880";

    private String apiKey = "devkey";

    private String apiSecret = "secret";

    /** Token 有效期（秒） */
    private int tokenTtlSec = 7200;
}
