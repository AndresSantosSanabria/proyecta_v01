package com.proyecta.api_gestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseRetryConfig implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseRetryConfig.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            int maxRetries = 20;
            int retries = 0;
            long waitInterval = 3000; // 3 seconds
            boolean connected = false;

            while (!connected && retries < maxRetries) {
                try (var _ = dataSource.getConnection()) {
                    logger.info("Database connection is ready.");
                    connected = true;
                } catch (Exception e) {
                    retries++;
                    logger.warn("Database not ready yet (Attempt {}/{}). Retrying in {} ms... ({})",
                            retries, maxRetries, waitInterval, e.getMessage());
                    if (retries >= maxRetries) {
                        logger.error("Failed to connect to the database after {} attempts.", maxRetries);
                    } else {
                        sleepQuietly(waitInterval);
                    }
                }
            }
        }
        return bean;
    }

    private void sleepQuietly(long waitInterval) {
        try {
            Thread.sleep(waitInterval);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}

