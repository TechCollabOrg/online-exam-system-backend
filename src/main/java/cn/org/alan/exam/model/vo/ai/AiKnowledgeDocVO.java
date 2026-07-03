package cn.org.alan.exam.model.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiKnowledgeDocVO {

    private Integer id;

    private String title;

    private String content;

    private Boolean enabled;

    private Integer sortOrder;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
