package cn.org.alan.exam.common.handler;

import cn.org.alan.exam.utils.DateTimeUtil;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * MyBatis-Plus 元对象处理器：在 <strong>插入</strong> 时按实体<strong>属性名</strong>（非表列名）自动填充
 * {@code userId}（当前登录用户）、{@code createTime}（服务器时间）；字段已有值则不覆盖。
 * <p>类名 {@code FiledFullHandler} 为历史拼写，与配置类 {@link cn.org.alan.exam.config.MybatisPlusConfig} 中 Bean 引用一致。</p>
 *
 * @author WeiJin
 */
@Component
@Slf4j
public class FiledFullHandler implements MetaObjectHandler {
    /**
     * INSERT 语句填充：反射遍历实体声明字段，匹配规则则 {@link #strictInsertFill}。
     *
     * @param metaObject 当前持久化对象元数据
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 没有创建人id就给他自动填充 放属性名而不是字段名
        Class<?> clazz = metaObject.getOriginalObject().getClass();
        Field[] fields = clazz.getDeclaredFields();
        Arrays.stream(fields).forEach(field -> {
            // 填充创建人
            if ("userId".equals(field.getName()) && (Objects.isNull(getFieldValByName("userId", metaObject)))) {
                log.info("user_id字段满足公共字段自动填充规则，已填充");
                this.strictInsertFill(metaObject, "userId", Integer.class, SecurityUtil.getUserId());

            }
            // 填充创建时间
            if ("createTime".equals(field.getName()) && (Objects.isNull(getFieldValByName("createTime", metaObject)))) {
                log.info("create_time字段满足公共字段自动填充规则，已填充");
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, DateTimeUtil.getDateTime());
            }
        });
    }

    /**
     * UPDATE 全局填充：当前项目未实现（如 {@code updateTime}），留空。
     *
     * @param metaObject 当前持久化对象元数据
     */
    @Override
    public void updateFill(MetaObject metaObject) {

    }
}
