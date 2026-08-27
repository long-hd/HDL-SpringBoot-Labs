package com.hdl.springboot.labs.lab28.task.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Hai {@link DataSource}: business ({@code user}) và Quartz ({@code quartz}).
 *
 * <p>Bean {@code quartzDataSource} phải có {@link QuartzDataSource} — Boot 3 tự gắn vào
 * {@code SchedulerFactoryBean} (không khai báo {@code jobStore.dataSource} trong yaml).
 *
 * <p>Prefix yaml: {@code spring.datasource.user.*}, {@code spring.datasource.quartz.*}.
 */
@Configuration
public class DataSourceConfig {

    /**
     * Cấu hình metadata cho DB user (business giả lập).
     */
    @Primary
    @Bean(name = "userDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.user")
    public DataSourceProperties userDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Pool JDBC chính của app — {@link Primary}.
     */
    @Primary
    @Bean(name = "userDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.user.hikari")
    public DataSource userDataSource() {
        return createHikariDataSource(userDataSourceProperties());
    }

    @Bean(name = "quartzDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.quartz")
    public DataSourceProperties quartzDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Pool JDBC chỉ dùng cho Quartz JobStore (bảng {@code QRTZ_*}).
     */
    @Bean(name = "quartzDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.quartz.hikari")
    @QuartzDataSource
    public DataSource quartzDataSource() {
        return createHikariDataSource(quartzDataSourceProperties());
    }

    private static HikariDataSource createHikariDataSource(DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        if (StringUtils.hasText(properties.getName())) {
            dataSource.setPoolName(properties.getName());
        }
        return dataSource;
    }

}
