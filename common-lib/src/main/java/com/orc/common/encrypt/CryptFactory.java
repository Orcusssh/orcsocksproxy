package com.orc.common.encrypt;

import com.orc.common.encrypt.impl.AesCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class CryptFactory {

    private static Logger logger = LoggerFactory.getLogger(CryptFactory.class);

    private static Map<String, String> crypts = new HashMap<String, String>();

    static {
        crypts.putAll(AesCrypt.getCiphers());
    }

    public static ICrypt get(String name, String password) {
        String className = crypts.get(name);
        if (className == null) {
            return null;
        }

        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(String.class,
                    String.class);
            return (ICrypt) constructor.newInstance(name, password);
        } catch (Exception e) {
            logger.error("get crypt error", e);
        }

        return null;
    }

}
