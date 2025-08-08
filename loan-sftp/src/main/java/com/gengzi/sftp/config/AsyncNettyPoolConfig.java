package com.gengzi.sftp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "s3.async-netty-pool")
public class AsyncNettyPoolConfig {


    private int coreThreads;
    private int maxThreads;
    private int connectionTimeout;
    private int connectionIdleTimeout;
    private int maxPendingConnectionAcquires;

    public int getCoreThreads() {
        return coreThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getConnectionIdleTimeout() {
        return connectionIdleTimeout;
    }

    public int getMaxPendingConnectionAcquires() {
        return maxPendingConnectionAcquires;
    }

    public void setCoreThreads(int coreThreads) {
        this.coreThreads = coreThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setConnectionIdleTimeout(int connectionIdleTimeout) {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }

    public void setMaxPendingConnectionAcquires(int maxPendingConnectionAcquires) {
        this.maxPendingConnectionAcquires = maxPendingConnectionAcquires;
    }
}
