package com.orc.common.auth;

import java.util.List;

public class AuthConfiguration {

    private Boolean open;//认证打开

    private List<AuthUser> users;

    public boolean auth(String user, String password){
        for (AuthUser authUser : users) {
            if(authUser.getUser().equalsIgnoreCase(user) && authUser.getPassword().equals(password)){
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

    public List<AuthUser> getUsers() {
        return users;
    }

    public void setUsers(List<AuthUser> users) {
        this.users = users;
    }

}
