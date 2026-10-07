package com.proyecta.api_gestion.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

@Configuration
@ConditionalOnBean(HikariDataSource.class)
public class DataSourceMonitorConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceMonitorConfig.class);

    private final DataSource dataSource;

    public DataSourceMonitorConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void logPoolStats() {
        if (dataSource instanceof HikariDataSource hikari) {
            var pool = hikari.getHikariPoolMXBean();
            log.info("[HikariPool] active={}, idle={}, waiting={}, total={}, timeout={}ms",
                    pool.getActiveConnections(),
                    pool.getIdleConnections(),
                    pool.getThreadsAwaitingConnection(),
                    pool.getTotalConnections(),
                    hikari.getConnectionTimeout());
        }
    }
}
