package com.orc.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ClientProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientProxyApplication.class, args);
    }

}
