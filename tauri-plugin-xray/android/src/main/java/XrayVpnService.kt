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
    
        builder.setMtu(1500)
        builder.setBlocking(true)
      
        // IPv4
        builder.addAddress("172.19.0.1", 30)
        builder.addRoute("0.0.0.0", 0)
    
    // IPv6
        builder.addAddress("fc00::", 126)
        builder.addRoute("::", 0)
    
        builder.addDnsServer("1.1.1.1")
    
        try {
            // Исключаем само приложение (VPN loop fix)
            builder.addDisallowedApplication(applicationContext.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    
        vpnInterface = builder.establish()
    }
    
    private fun startXray(configPath: String) {
        LibXray.registerDialerController(this)
        
        val fd = vpnInterface!!.fd
        try {
            android.system.Os.setenv("xray.tun.fd", fd.toString(), true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val datDir = filesDir.absolutePath // Убедитесь, что файлы .dat тут есть!

        Thread {
            try {
                val req = LibXray.newXrayRunRequest(datDir, datDir, configPath)
                val resultBase64 = LibXray.runXray(req)
                
                // Если ядро упало или вернуло ошибку при старте:
                if (resultBase64.isNotEmpty()) {
                    val resultJson = String(android.util.Base64.decode(resultBase64, android.util.Base64.DEFAULT))
                    android.util.Log.e("XRAY_CORE", "FATAL ERROR: $resultJson")
                }
            } catch (e: Exception) {
                android.util.Log.e("XRAY_CORE", "Crash", e)
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
