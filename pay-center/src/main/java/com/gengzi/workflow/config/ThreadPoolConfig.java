package com.gengzi.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置
 * 
 * 配置说明:
 * - corePoolSize: 核心线程数 = CPU 核数 * 2 (I/O 密集型)
 * - maxPoolSize: 最大线程数 = 50
 * - keepAliveTime: 空闲线程存活时间 = 60s
 * - queue: LinkedBlockingQueue (容量 1000)
 * 
 * @author gengzi
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService workflowExecutorService() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maxPoolSize = 50;
        long keepAliveTime = 60L;

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略: 调用者运行
        );
    }
}
