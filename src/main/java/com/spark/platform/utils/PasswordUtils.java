package com.spark.platform.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    private static final int BCRYPT_ROUNDS = 10;

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}