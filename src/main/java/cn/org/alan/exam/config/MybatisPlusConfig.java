package cn.org.alan.exam.config;

import cn.org.alan.exam.common.handler.FiledFullHandler;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import javax.annotation.Resource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus：扫描 {@code cn.org.alan.exam.mapper}、注册 MySQL 分页插件，
 * 并将 {@link FiledFullHandler} 注册为全局元对象处理器（插入时自动填充等）。
 *
 * @author Alan
 */
@Configuration
@MapperScan("cn.org.alan.exam.mapper")
public class MybatisPlusConfig {
    @Resource
    private FiledFullHandler filedFullHandler;

    /**
     * 拦截器链：启用针对 MySQL 的物理分页（{@link PaginationInnerInterceptor}）。
     *
     * @return MyBatis-Plus 插件总线
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new
                PaginationInnerInterceptor(DbType.MYSQL));
        // 添加元数据对象处理器
        return interceptor;
    }

    /**
     * 全局配置：挂载插入/更新时的字段自动填充处理器。
     *
     * @return MyBatis-Plus {@link GlobalConfig}
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig config = new GlobalConfig();
        config.setMetaObjectHandler(filedFullHandler);
        return config;
    }
}
