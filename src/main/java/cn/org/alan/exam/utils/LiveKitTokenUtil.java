package cn.org.alan.exam.utils;

import cn.org.alan.exam.config.LiveKitProperties;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 生成 LiveKit 房间 JWT（HS256，video grant）。
 */
@Component
public class LiveKitTokenUtil {

    @Resource
    private LiveKitProperties liveKitProperties;

    public static String roomName(Integer examId) {
        return "oes-exam-" + examId;
    }

    public static String studentIdentity(Integer userId) {
        return "student-" + userId;
    }

    public static String proctorIdentity(Integer userId) {
        return "proctor-" + userId;
    }

    public String createToken(String identity, Integer examId, boolean canPublish, boolean canSubscribe) {
        String room = roomName(examId);
        Map<String, Object> video = new HashMap<>();
        video.put("roomJoin", true);
        video.put("room", room);
        if (canPublish) {
            video.put("canPublish", true);
        }
        if (canSubscribe) {
            video.put("canSubscribe", true);
        }

        Date now = new Date();
        Date exp = new Date(now.getTime() + liveKitProperties.getTokenTtlSec() * 1000L);

        return JWT.create()
                .withIssuer(liveKitProperties.getApiKey())
                .withSubject(identity)
                .withClaim("video", video)
                .withIssuedAt(now)
                .withNotBefore(now)
                .withExpiresAt(exp)
                .withJWTId(UUID.randomUUID().toString())
                .sign(Algorithm.HMAC256(liveKitProperties.getApiSecret()));
    }
}
