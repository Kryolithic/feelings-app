package com.example.feelingsapp.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.firestore
import java.util.concurrent.atomic.AtomicBoolean

object FirebaseConfig {
    private val securityConfigured = AtomicBoolean(false)

    fun isConfigured(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
        } catch (_: Exception) {
            false
        }
    }

    fun configureSecurityDefaults(context: Context) {
        if (!isConfigured(context)) return
        if (!securityConfigured.compareAndSet(false, true)) return

        AppCheckInitializer.install()
        runCatching {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            Firebase.firestore.firestoreSettings = settings
        }
    }

    fun getWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return context.getString(resId).takeIf { it.isNotBlank() }
    }
}
