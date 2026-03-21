package com.plugin.xray

import android.app.Activity
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

    @Command
    fun ping(invoke: Invoke) {
        val args = invoke.parseArgs(PingArgs::class.java)
        val ret = JSObject()

        try {
            // Распаковываем JSON из поля value
            val json = JSONObject(args.value ?: "{}")
            val action = json.optString("action", "unknown")
            val data = json.optString("data", "")

            when (action) {
                "startVpn" -> ret.put("value", "VPN started with config: $data")
                "stopVpn" -> ret.put("value", "VPN stopped")
                "getStatus" -> ret.put("value", "Disconnected")
                else -> ret.put("value", "Error: Unknown action '$action'")
            }
        } catch (e: Exception) {
            ret.put("value", "Error parsing JSON: ${e.message}")
        }
        
        invoke.resolve(ret)
    }
}