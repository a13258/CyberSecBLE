package com.mei.cybersecble.BLE

import android.annotation.SuppressLint
import com.mei.cybersecble.Utils.Constants.Companion.globalAuthKey
import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.or

const val AUTH_SEND_KEY: Byte = 0x01
const val AUTH_REQUEST_RANDOM_AUTH_NUMBER: Byte = 0x02
const val AUTH_SEND_ENCRYPTED_AUTH_NUMBER: Byte = 0x03
const val AUTH_RESPONSE: Byte = 0x10
const val AUTH_SUCCESS: Byte = 0x01

val startHeartMeasurementContinuous = byteArrayOf(0x15, 0x1, 1)
val stopHeartMeasurementContinuous = byteArrayOf(0x15, 0x1, 0)
val stopHeartMeasurementSingle = byteArrayOf(0x15, 0x2, 0)


    fun getAuthFlags(): Byte {
        return 0x00
    }

    fun getCryptFlags(): Byte {
        return 0x80.toByte()
    }


    fun requestAuthNumber(): ByteArray {
        var cryptFlags = getCryptFlags()
        var authFlags = getAuthFlags()
        return if (cryptFlags === 0x00.toByte()) {
            byteArrayOf(AUTH_REQUEST_RANDOM_AUTH_NUMBER, authFlags)
        } else {
            byteArrayOf(
                (cryptFlags or AUTH_REQUEST_RANDOM_AUTH_NUMBER),
                authFlags,
                0x02,
                0x01,
                0x00
            )
        }
    }

    fun getSecretKey(): ByteArray {
        val authKeyBytes = byteArrayOf(
            0x30,
            0x31,
            0x32,
            0x33,
            0x34,
            0x35,
            0x36,
            0x37,
            0x38,
            0x39,
            0x40,
            0x41,
            0x42,
            0x43,
            0x44,
            0x45
        )

        //val authKey = "8e3979b21287082f7bfc7a8a63d8b34f"
        var authKey = globalAuthKey

        if (authKey != null && !authKey.isEmpty()) {
            if (checkHex(authKey)) {
                var srcBytes = authKey.trim { it <= ' ' }.toByteArray()
                if(authKey.length == 32) {
                    srcBytes = authKey.decodeHex()
                }
                System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.size, 16))
            }
        }
        return authKeyBytes
    }

    fun checkHex(s: String): Boolean {
        // Size of string
        val n = s.length

        // Iterate over string
        for (i in 0 until n) {
            val ch = s[i]

            // Check if the character
            // is invalid
            if ((ch < '0' || ch > '9')
                && (ch < 'A' || ch > 'F')
            ) {
                return false
            }
        }
        return true
    }

    @Throws(
        InvalidKeyException::class,
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    fun handleAESAuth(value: ByteArray, secretKey: ByteArray): ByteArray? {
        val mValue = Arrays.copyOfRange(value, 3, 19)
        @SuppressLint("GetInstance") val ecipher: Cipher =
            Cipher.getInstance("AES/ECB/NoPadding")
        val newKey = SecretKeySpec(secretKey, "AES")
        ecipher.init(Cipher.ENCRYPT_MODE, newKey)
        return ecipher.doFinal(mValue)
    }

    fun concat(vararg arrays: ByteArray): ByteArray {
        val n = arrays.map {it.size}.sum()
        val byteBuffer = ByteBuffer.allocate(n)
        arrays.forEach { byteBuffer.put(it) }
        return byteBuffer.array()
    }

    fun byteArrayToHex(a: ByteArray): String {
        val sb = StringBuilder(a.size * 2)
        for (b in a) sb.append(String.format("%02x", b))
        return sb.toString()
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
