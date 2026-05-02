package cn.org.alan.exam.model.vo.stat;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户按日的在线时长曲线点（日期与时长）。
 *
 * @author Alan
 * @since 2024/5/29
 */
@Data
public class DailyVO {
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 登录日期
     */
    private LocalDate loginDate;

    /**
     * 累积在线秒数
     */
    private Integer totalSeconds;
}
