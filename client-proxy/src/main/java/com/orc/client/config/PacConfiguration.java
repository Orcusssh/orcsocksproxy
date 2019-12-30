package com.orc.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties("pac")
@Component
public class PacConfiguration {

    private Boolean open;

    private List<String> domains;

    public boolean checkInPac(String domain){
        for (String s : domains) {
            if(s.contains(domain) || domain.contains(s)){
                return true;
            }
        }
        return false;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

}
