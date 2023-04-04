package com.example.config

import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.util.DigestUtils

class AppPasswordEncoder: PasswordEncoder {
    private val pe = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    override fun encode(rawPassword: CharSequence): String {
        return pe.encode(rawPassword)
    }

    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean {
        return DigestUtils.md5DigestAsHex(rawPassword.toString().toByteArray()) == encodedPassword ||
                pe.matches(rawPassword, encodedPassword)
    }
}