package org.bitly.util;

import org.springframework.security.crypto.bcrypt.BCrypt;


public class NUtil {

    private NUtil(){
        throw new IllegalArgumentException("It is a utility class.");
    }

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }

}
