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
            val data = json.optString("data", "") 

            when (action) {
                "startVpn" -> {
                    val configPath = data
                    if (java.io.File(configPath).exists()) {
                        VpnController.start(activity.applicationContext, configPath)
                        ret.put("value", "VPN start initiated")
                    } else {
                        ret.put("value", "Ошибка: файл конфига не найден по пути $configPath")
                    }
                }
                "stopVpn" -> {
                    VpnController.stop(activity.applicationContext)
                    ret.put("value", "VPN stopped")
                }
                "getStatus" -> {
                    val isActive = VpnController.getStatus()
                    ret.put("value", if (isActive) "Connected" else "Disconnected")
                }
                "getLogs" -> {
                    val logFile = java.io.File("/storage/emulated/0/Android/data/com.pro100.vpnapp/files/error.log")
                    if (logFile.exists()) {
                        val logs = logFile.readText()
                        ret.put("value", if (logs.length > 5000) logs.takeLast(5000) else logs)
                    } else {
                        ret.put("value", "Файл логов пока не создан.")
                    }
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
