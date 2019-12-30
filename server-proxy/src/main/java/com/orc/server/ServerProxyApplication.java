package com.orc.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ServerProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerProxyApplication.class, args);
    }

}
