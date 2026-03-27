package com.plugin.xray

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import androidx.annotation.Keep
import libXray.LibXray
import libXray.DialerController
import org.json.JSONObject

@Keep
class XrayVpnService : VpnService(), DialerController {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun protectFd(fd: Long): Boolean {
        return protect(fd.toInt())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        val configPath = intent?.getStringExtra("configPath") ?: return START_NOT_STICKY
        val configJson = intent?.getStringExtra("configJson") ?: ""
        
        startCore(configPath, configJson)
        
        return START_STICKY
    }

    private fun startCore(configPath: String, configJson: String) {
        LibXray.registerDialerController(this)

        val builder = Builder()
            .setMtu(1500)
            .setBlocking(true)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addAddress("fc00::", 126)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            
        try {
            builder.addDisallowedApplication(applicationContext.packageName)
        } catch (e: Exception) {}

        vpnInterface = builder.establish()
        
        if (vpnInterface == null) return

        val fd = vpnInterface!!.detachFd()
        
        try {
            android.system.Os.setenv("xray.tun.fd", fd.toString(), true)
        } catch (e: Exception) {
            Log.e("XRAY_CORE", "Failed to set env xray.tun.fd", e)
        }

        val datDir = filesDir.absolutePath

        Thread {
            try {
                val requestObj = JSONObject()
                requestObj.put("datDir", datDir)
                requestObj.put("mphCachePath", datDir)
                
                if (configJson.isNotEmpty()) {
                    requestObj.put("configJSON", configJson)
                } else {
                    requestObj.put("configPath", configPath)
                }

                val reqBase64 = Base64.encodeToString(
                    requestObj.toString().toByteArray(), 
                    Base64.NO_WRAP
                )
                
                val resultBase64 = if (configJson.isNotEmpty()) {
                    LibXray.runXrayFromJSON(fd.toLong(), reqBase64)
                } else {
                    LibXray.runXray(fd.toLong(), reqBase64)
                }
                
                if (resultBase64.isNotEmpty()) {
                    val error = String(Base64.decode(resultBase64, Base64.DEFAULT))
                    Log.e("XRAY_CORE", "Error: $error")
                }
            } catch (e: Exception) {
                Log.e("XRAY_CORE", "Crash", e)
            }
        }.start()
    }

    private fun stopVpn() {
        try {
            LibXray.stopXray()
        } catch (e: Exception) {}
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDe
        stroy()
    }
}
