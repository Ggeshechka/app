package com.plugin.xray

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.Os
import android.util.Base64
import libXray.LibXray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class XrayVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_VPN") {
            LibXray.stopXray()
            stopSelf()
            return Service.START_NOT_STICKY
        }

        setupVpn()
        return Service.START_STICKY
    }

    private fun setupVpn() {
        if (vpnInterface != null) return

        val builder = Builder()
        builder.setSession("Xray VPN")
               .addAddress("10.0.0.2", 24)
               .addRoute("0.0.0.0", 0)
               
        try {
            vpnInterface = builder.establish()
            val fd = vpnInterface?.fd
            
            if (fd != null) {
                Os.setenv("XRAY_TUN_FD", fd.toString(), true)
                Os.setenv("xray.tun.fd", fd.toString(), true)

                Thread {
                    try {
                        copyAsset(applicationContext, "geoip.dat")
                        copyAsset(applicationContext, "geosite.dat")
                        copyAsset(applicationContext, "config.json")

                        val configFile = File(applicationContext.filesDir, "config.json")

                        val jsonRequest = JSONObject().apply {
                            put("datDir", applicationContext.filesDir.absolutePath)
                            put("mphCachePath", File(applicationContext.cacheDir, "mph.cache").absolutePath)
                            put("configPath", configFile.absolutePath)
                        }.toString()

                        val base64Request = Base64.encodeToString(jsonRequest.toByteArray(), Base64.NO_WRAP)
                        
                        LibXray.buildMphCache(base64Request)
                        
                        val resultBase64 = LibXray.runXray(base64Request)
                        val resultStr = String(Base64.decode(resultBase64, Base64.NO_WRAP))
                        println("Xray core result: $resultStr")

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyAsset(context: Context, filename: String) {
        val outFile = File(context.filesDir, filename)
        if (!outFile.exists()) {
            var inputStream: InputStream? = null
            var out: FileOutputStream? = null
            try {
                inputStream = context.assets.open(filename)
                out = FileOutputStream(outFile)
                val buffer = ByteArray(1024)
                var read = inputStream.read(buffer)
                while (read != -1) {
                    out.write(buffer, 0, read)
                    read = inputStream.read(buffer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inputStream?.close()
                out?.flush()
                out?.close()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LibXray.stopXray()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface = null
    }
}