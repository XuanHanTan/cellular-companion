package com.xuanhan.cellularcompanion.utilities

import com.ramcosta.composedestinations.navargs.utils.toBase64Str
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AES {
    private var key: SecretKeySpec? = null
    private val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

    fun initialize(password: String) {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val bytes = password.toByteArray()
        digest.update(bytes, 0, bytes.size)
        key = SecretKeySpec(digest.digest(), "AES")
    }

    private fun encrypt(plaintext: String): Pair<String, String> {
        if (key == null) {
            throw UnsupportedOperationException("Initialise AES class before calling encrypt()!")
        }

        val plainText = plaintext.toByteArray(Charsets.UTF_8)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val cipherText = cipher.doFinal(plainText)
        val iv = cipher.iv
        return Pair(cipherText.toBase64Str(), iv.toBase64Str())
    }
}