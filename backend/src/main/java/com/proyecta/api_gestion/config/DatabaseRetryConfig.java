package com.proyecta.api_gestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class DatabaseRetryConfig implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseRetryConfig.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            DataSource dataSource = (DataSource) bean;
            int maxRetries = 20;
            int retries = 0;
            long waitInterval = 3000; // 3 seconds

            while (retries < maxRetries) {
                try (Connection conn = dataSource.getConnection()) {
                    logger.info("Database connection is ready.");
                    break;
                } catch (Exception e) {
                    retries++;
                    logger.warn("Database not ready yet (Attempt {}/{}). Retrying in {} ms... ({})", 
                            retries, maxRetries, waitInterval, e.getMessage());
                    if (retries >= maxRetries) {
                        logger.error("Failed to connect to the database after {} attempts.", maxRetries);
                        break;
                    }
                    try {
                        Thread.sleep(waitInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return bean;
    }
}
