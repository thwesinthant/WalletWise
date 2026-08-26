package com.example.walletwise.util

import java.security.MessageDigest

object PasswordUtils {

    /**
     * One-way hash for storing/comparing passwords.
     * NOTE: For a real production app you'd want salting + a slow hash
     * (bcrypt/Argon2). SHA-256 is fine for a student project since Room's
     * SQLite file is on-device and this isn't a security-critical deployment.
     */
    fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}