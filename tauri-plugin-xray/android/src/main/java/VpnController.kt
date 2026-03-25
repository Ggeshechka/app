package com.plugin.xray

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import libXray.LibXray // Зависит от вашего пакета gomobile

object VpnController {
    fun start(context: Context, configPath: String) {
        if (getStatus()) return

        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) {
            // Запрос прав
            val intent = Intent(context, VpnPermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("configPath", configPath)
            }
            context.startActivity(intent)
            return
        }
        
        startService(context, configPath)
    }

    fun startService(context: Context, configPath: String) {
        val serviceIntent = Intent(context, XrayVpnService::class.java).apply {
            action = "START"
            putExtra("configPath", configPath)
        }
        context.startService(serviceIntent)
    }

    fun stop(context: Context) {
        val serviceIntent = Intent(context, XrayVpnService::class.java).apply {
            action = "STOP"
        }
        context.startService(serviceIntent)
    }

    fun getStatus(): Boolean {
        return try {
            LibXray.getXrayState() // Или ваш метод проверки статуса из libXray
        } catch (e: Exception) {
            false
        }
    }
}

class VpnPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, 1)
        } else {
            proceed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK) proceed()
        finish()
    }

    private fun proceed() {
        val config = intent.getStringExtra("configPath") ?: return
        VpnController.startService(this, config)
        finish()
    }
}