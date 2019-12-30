package com.orc.common.message;

import org.msgpack.annotation.Message;

import java.util.List;

@Message
public class AuthResponseMessage implements com.orc.common.message.Message {


    private byte authResult;


    public byte getAuthResult() {
        return authResult;
    }

    public void setAuthResult(byte authResult) {
        this.authResult = authResult;
    }

    public boolean isAuthSuccess(){
        if(AuthResult.getByResult(authResult) == AuthResult.SUCCESS){
            return true;
        }
        return false;
    }

    public enum AuthResult {

        FAILURE((byte)0x01, "失败"),
        SUCCESS((byte)0x02, "成功");

        private final byte result;
        private final String desc;

        AuthResult( byte result, String desc){
            this.result = result;
            this.desc = desc;
        }
        
        public static AuthResult getByResult(byte result){
            for (AuthResult value : AuthResult.values()) {
                if(result == value.getResult()){
                    return value;
                }
            }
            return null;
        }

        public byte getResult() {
            return result;
        }

        public String getDesc() {
            return desc;
        }
    }
}
