package com.yalantis.ucrop.sample

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yalantis.ucrop.UCropHttpClientStore
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setUcropHttpClient()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is BaseActivity) {
                    activity.enableEdgeToEdge()
                    ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { v, windowInsets ->
                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                        v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                        windowInsets
                    }
                }
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        })
    }

    private fun setUcropHttpClient() {
        val cs: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .allEnabledCipherSuites()
            .allEnabledTlsVersions()
            .build()
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(cs))
            .build()
        UCropHttpClientStore.INSTANCE.client = client
    }
}