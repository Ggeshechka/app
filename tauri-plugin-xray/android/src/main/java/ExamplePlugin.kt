package com.plugin.xray

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.result.ActivityResult
import app.tauri.annotation.ActivityCallback
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke
import org.json.JSONObject

@InvokeArg
class PingArgs {
    var value: String? = null
}

@TauriPlugin
class ExamplePlugin(private val activity: Activity): Plugin(activity) {

    private var pendingConfigData: String = ""

    @Command
    fun ping(invoke: Invoke) {
        val args = invoke.parseArgs(PingArgs::class.java)
        val ret = JSObject()

        try {
            val json = JSONObject(args.value ?: "{}")
            val action = json.optString("action", "unknown")
            val data = json.optString("data", "")

            when (action) {
                "startVpn" -> checkAndStartVpn(invoke, data)
                "stopVpn" -> {
                    val intent = Intent(activity, XrayVpnService::class.java)
                    intent.action = "STOP_VPN"
                    activity.startService(intent)
                    
                    ret.put("value", "VPN stopped")
                    invoke.resolve(ret)
                }
                "getStatus" -> {
                    ret.put("value", "Disconnected")
                    invoke.resolve(ret)
                }
                else -> {
                    ret.put("value", "Error: Unknown action '$action'")
                    invoke.resolve(ret)
                }
            }
        } catch (e: Exception) {
            ret.put("value", "Error parsing JSON: ${e.message}")
            invoke.resolve(ret)
        }
    }

    private fun checkAndStartVpn(invoke: Invoke, data: String) {
        val intent = VpnService.prepare(activity)
        
        if (intent != null) {
            pendingConfigData = data
            startActivityForResult(invoke, intent, "onVpnPermissionResult")
        } else {
            startVpnCore(invoke, data)
        }
    }

    @ActivityCallback
    fun onVpnPermissionResult(invoke: Invoke, result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnCore(invoke, pendingConfigData)
        } else {
            val ret = JSObject()
            ret.put("value", "Error: VPN permission denied by user")
            invoke.resolve(ret)
        }
        pendingConfigData = ""
    }

    private fun startVpnCore(invoke: Invoke, data: String) {
        val intent = Intent(activity, XrayVpnService::class.java)
        intent.action = "START_VPN"
        activity.startService(intent)

        val ret = JSObject()
        ret.put("value", "VPN service starting with assets/config.json")
        invoke.resolve(ret)
    }
}