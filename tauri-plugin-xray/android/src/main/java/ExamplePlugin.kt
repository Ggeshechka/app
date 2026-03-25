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
            val json = JSONObject(args.value ?: "{}")
            val action = json.optString("action", "unknown")
            val data = json.optString("data", "") // Ожидается путь к конфигу

            when (action) {
                "startVpn" -> {
                    // data содержит сырой JSON, сохраняем его в config.json
                    val configFile = java.io.File(activity.filesDir, "config.json")
                    configFile.writeText(data)
                    
                    // Передаем абсолютный путь к созданному файлу
                    VpnController.start(activity.applicationContext, configFile.absolutePath)
                    ret.put("value", "VPN start initiated")
                }
                "stopVpn" -> {
                    VpnController.stop(activity.applicationContext)
                    ret.put("value", "VPN stopped")
                }
                "getStatus" -> {
                    val isActive = VpnController.getStatus()
                    ret.put("value", if (isActive) "Connected" else "Disconnected")
                }
                else -> {
                    ret.put("value", "Error: Unknown action '$action'")
                }
            }
            invoke.resolve(ret)
        } catch (e: Exception) {
            ret.put("value", "Error parsing JSON: ${e.message}")
            invoke.resolve(ret)
        }
    }
}