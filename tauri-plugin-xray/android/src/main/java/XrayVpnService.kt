package com.plugin.xray

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import libXray.LibXray
import libXray.DialerController
import androidx.annotation.Keep 
import android.system.Os

@Keep
class XrayVpnService : VpnService(), DialerController {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun protectFd(fd: Long): Boolean {
        return protect(fd.toInt()) // Приводим Long к Int, так как VpnService.protect требует Int
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        val configPath = intent?.getStringExtra("configPath") ?: return START_NOT_STICKY
        
        setupVpn()
        startXray(configPath)
        
        return START_STICKY
    }

    private fun setupVpn() {
        if (vpnInterface != null) return
        val builder = Builder()
        
        // Базовые настройки TUN (IP и маршрутизация)
        builder.addAddress("172.19.0.1", 30)
        builder.addDnsServer("8.8.8.8")
        builder.addRoute("0.0.0.0", 0) 
        
        vpnInterface = builder.establish()
    }

    private fun startXray(configPath: String) {
        LibXray.registerDialerController(this)
        
        val fd = vpnInterface!!.fd
        try {
            Os.setenv("xray.tun.fd", fd.toString(), true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val datDir = filesDir.absolutePath

        Thread {
            try {
                val req = LibXray.newXrayRunRequest(datDir, datDir, configPath)
                val resultBase64 = LibXray.runXray(req)
                
                if (resultBase64.isNotEmpty()) {
                    val resultJson = String(android.util.Base64.decode(resultBase64, android.util.Base64.DEFAULT))
                    android.util.Log.e("XRAY_CORE", "Result: $resultJson")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            LibXray.stopXray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface?.close()
        vpnInterface = null
    }
}