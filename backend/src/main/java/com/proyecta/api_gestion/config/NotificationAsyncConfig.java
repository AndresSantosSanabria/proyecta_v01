package com.proyecta.api_gestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class NotificationAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationAsyncConfig.class);

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        return createExecutor("notificationExecutor", 2, 6, 50, "notif-");
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return createExecutor("taskExecutor", 4, 10, 100, "async-");
    }

    private Executor createExecutor(String beanName, int core, int max, int queue, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix(prefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("[ThreadPool] {} initialized — core={}, max={}, queue={}", beanName, core, max, queue);
        return executor;
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(15);
        log.info("[ThreadPool] taskScheduler initialized — pool=4");
        return scheduler;
    }
}
