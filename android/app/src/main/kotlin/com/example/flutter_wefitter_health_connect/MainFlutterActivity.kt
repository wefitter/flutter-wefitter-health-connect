package com.example.flutter_wefitter_health_connect

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import com.wefitter.healthconnect.WeFitterHealthConnect
import com.wefitter.healthconnect.WeFitterHealthConnectError
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.util.ViewUtils.getActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MainFlutterActivity: FlutterFragmentActivity() {

    companion object {
        private const val CHANNEL = "wefitter"
        private const val EVENT_CHANNEL = "wefitter_sdk"
    }

    private lateinit var activity: FragmentActivity
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: MethodChannel

    class WFConfigMap: HashMap<String, String>()

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        eventChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        activity = getActivity(this)!! as FragmentActivity
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "configure" -> {
                    val arguments = call.arguments as HashMap<*, *>
                    val config = arguments["config"] as HashMap<*, *>
                    val token = config["token"] as String
                    val wfConfig = WFConfigMap()
                    if ("token" in config.keys) wfConfig.put("token", config["token"] as String)
                    if ("apiUrl" in config.keys) wfConfig.put("apiUrl", config["apiUrl"] as String)
                    if ("startDate" in config.keys) wfConfig.put("startDate", config["startDate"] as String)
                    if ("notificationTitle" in config.keys) wfConfig.put("notificationTitle", config["notificationTitle"] as String)
                    if ("notificationText" in config.keys) wfConfig.put("notificationText", config["notificationText"] as String)
                    if ("notificationIcon" in config.keys) wfConfig.put("notificationIcon", config["notificationIcon"] as String)
                    if ("notificationChannelId" in config.keys) wfConfig.put("notificationChannelId", config["notificationChannelId"] as String)
                    if ("notificationChannelName" in config.keys) wfConfig.put("notificationChannelName", config["notificationChannelName"] as String)
                    if ("appPermissions" in config.keys)wfConfig.put("appPermissions", config["appPermissions"] as String)
                    configure(wfConfig)
                }
                "connect" -> {
                    connect()
                }
                "disconnect" -> {
                    disconnect()
                }
                "isSupported" -> {
                    val supported = weFitter.isSupported()
                    eventChannel.invokeMethod("onSupported",
                        mapOf<String, Boolean>("supported" to supported)
                    )
                }
                else -> Toast.makeText(this@MainFlutterActivity, "NOT IMPLEMENTED", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val weFitter by lazy { WeFitterHealthConnect(activity) }

    fun configure(config: WFConfigMap) {
        val token = config["token"] ?: ""
        val apiUrl = config["apiUrl"]
        val statusListener = object : WeFitterHealthConnect.StatusListener {
            override fun onConfigured(configured: Boolean) {
                eventChannel.invokeMethod("onConfiguredWeFitterHealthConnect",
                    mapOf<String, Boolean>("configured" to configured)
                )
            }

            override fun onConnected(connected: Boolean) {
                eventChannel.invokeMethod(
                    "onConnectedWeFitterHealthConnect",
                    mapOf<String, Boolean>("connected" to connected)
                )
            }

            override fun onError(error: WeFitterHealthConnectError) {
                eventChannel.invokeMethod("onErrorWeFitterHealthConnect",
                    mapOf<String, String>("error" to error.message)
                )
            }
        }
        val notificationConfig = parseNotificationConfig(config)
        val startDate = parseStartDate(config)
        val appPermissions = parseAppPermission(config)

        weFitter.configure(token, apiUrl, statusListener, notificationConfig, startDate, appPermissions)
    }

    fun connect() {
        weFitter.connect()
    }

    fun disconnect() {
        weFitter.disconnect()
    }

    /*
    fun isConnected(callback: Callback) {
        callback(weFitter.isConnected())
    }

    fun isSupported(callback: Callback) {
        callback(weFitter.isSupported())
    }
     */

    private fun parseNotificationConfig(config: WFConfigMap): WeFitterHealthConnect.NotificationConfig {
        return WeFitterHealthConnect.NotificationConfig().apply {
            config["notificationTitle"]?.let { title = it }
            config["notificationText"]?.let { text = it }
            config["notificationIcon"]?.let {
                val resourceId = getResourceId(it)
                if (resourceId != 0) iconResourceId = resourceId
            }
            config["notificationChannelId"]?.let { channelId = it }
            config["notificationChannelName"]?.let { channelName = it }
        }
    }

    private fun parseStartDate(config: WFConfigMap): Date? {
        val startDateString = config["startDate"]
        if (startDateString != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.parse(startDateString)
        }
        return null
    }

    private fun getResourceId(resourceName: String): Int {
        val resources = this.resources
        val packageName = this.packageName
        var resourceId = resources.getIdentifier(resourceName, "mipmap", packageName)
        if (resourceId == 0) {
            resourceId = resources.getIdentifier(resourceName, "drawable", packageName)
        }
        if (resourceId == 0) {
            resourceId = resources.getIdentifier(resourceName, "raw", packageName)
        }
        return resourceId
    }

    private fun parseAppPermission(config: WFConfigMap): Set<String> {
        val appPermsString: String? = config["appPermissions"]
        if (appPermsString != null) {
            val appPerms: Set<String> = appPermsString.split(',').toSet()
            return appPerms
        }
        return emptySet()
    }

}
