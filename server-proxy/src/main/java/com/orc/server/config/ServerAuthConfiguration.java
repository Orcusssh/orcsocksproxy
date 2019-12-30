package com.orc.server.config;

import com.orc.common.auth.AuthConfiguration;
import com.orc.common.auth.AuthUser;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties("server.auth")
@Component
public class ServerAuthConfiguration extends AuthConfiguration {

    @Override
    public boolean auth(String user, String password){
        if(!getOpen()){//无需认证
            return true;
        }
        return super.auth(user, password);
    }
}
