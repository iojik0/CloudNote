package com.cloudnote.utils;

public class hashPasswordTest {
    public static void main(String[] args) {
        String password = "Snikers1901";
        String hashPass = PasswordHasher.hashPassword(password);

        System.out.println(password);
        System.out.println(hashPass);

    }
}
