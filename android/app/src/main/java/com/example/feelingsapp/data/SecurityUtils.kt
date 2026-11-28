package com.example.feelingsapp.data

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {
    const val MIN_SIGNUP_PASSWORD_LENGTH = 8
    const val MIN_SIGNIN_PASSWORD_LENGTH = 6
    const val MAX_NOTE_LENGTH = 2000

    private val secureRandom = SecureRandom()

    fun generateHashedNonce(): String {
        val rawBytes = ByteArray(32)
        secureRandom.nextBytes(rawBytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(rawBytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun validateSignUpPassword(password: String): String? {
        if (password.length < MIN_SIGNUP_PASSWORD_LENGTH) {
            return "Password must be at least $MIN_SIGNUP_PASSWORD_LENGTH characters."
        }
        if (password.isBlank()) {
            return "Password cannot be blank spaces."
        }
        return null
    }

    fun sanitizeNote(note: String): String = note.trim().take(MAX_NOTE_LENGTH)

    fun mapAuthErrorCode(errorCode: String?, isNetworkError: Boolean = false): String {
        if (isNetworkError || errorCode == "ERROR_NETWORK_REQUEST_FAILED") {
            return "Network error. Please check your connection and try again."
        }
        return when (errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND" -> "Incorrect email or password."
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Unable to create an account with this email. Try signing in or resetting your password."
            "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
            "ERROR_WEAK_PASSWORD" ->
                "Password does not meet security requirements (minimum $MIN_SIGNUP_PASSWORD_LENGTH characters)."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email sign-in is currently unavailable."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
            else -> "Authentication failed. Please try again."
        }
    }

    fun mapFirestoreErrorCode(codeName: String?, fallback: String): String {
        return when (codeName) {
            "PERMISSION_DENIED" -> "You do not have permission to access this data."
            "UNAVAILABLE" -> "Cloud sync is temporarily unavailable."
            "UNAUTHENTICATED" -> "Please sign in again to sync your logs."
            "DEADLINE_EXCEEDED" -> "Sync timed out. Please try again."
            "RESOURCE_EXHAUSTED" -> "Too many requests. Please wait a moment and try again."
            else -> fallback
        }
    }
}
