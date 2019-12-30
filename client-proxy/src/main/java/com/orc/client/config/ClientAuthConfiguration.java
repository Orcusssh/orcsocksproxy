package com.orc.client.config;

import com.orc.common.auth.AuthConfiguration;
import com.orc.common.auth.AuthUser;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties("client.auth")
@Component
public class ClientAuthConfiguration extends AuthConfiguration {
}
