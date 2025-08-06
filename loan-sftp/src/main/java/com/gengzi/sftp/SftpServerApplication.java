package com.gengzi.sftp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
public class SftpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SftpServerApplication.class, args);
    }
}