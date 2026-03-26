package com.plugin.xray

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import androidx.annotation.Keep
import libXray.LibXray
import libXray.DialerController

@Keep
class XrayVpnService : VpnService(), DialerController {

    private var vpnInterface: ParcelFileDescriptor? = null

    // Реализация интерфейса для защиты сокетов от зацикливания
    override fun protectFd(fd: Long): Boolean {
        return protect(fd.toInt())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        val configPath = intent?.getStringExtra("configPath") ?: return START_NOT_STICKY
        
        startCore(configPath)
        
        return START_STICKY
    }

    private fun startCore(configPath: String) {
        // 1. Регистрируем контроллер защиты сокетов
        LibXray.registerDialerController(this)

        // 2. Настраиваем VPN и получаем интерфейс
        val builder = Builder()
            .setMtu(1500)
            .setBlocking(true)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addAddress("fc00::", 126)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .addDisallowedApplication(applicationContext.packageName)

        vpnInterface = builder.establish()
        
        if (vpnInterface == null) {
            Log.e("XRAY_SERVICE", "Failed to establish VPN interface")
            return
        }

        // 3. Извлекаем FD и передаем владение в Go
        val fd = vpnInterface!!.detachFd()

        val datDir = filesDir.absolutePath

        Thread {
            try {
                // Генерируем запрос (используем существующий в либе метод или вручную JSON)
                val reqBase64 = LibXray.newXrayRunRequest(datDir, datDir, configPath)
                
                // ВАЖНО: Вызываем нашу новую функцию с FD
                val resultBase64 = LibXray.runXray(fd, reqBase64)
                
                if (resultBase64.isNotEmpty()) {
                    val errorJson = String(Base64.decode(resultBase64, Base64.DEFAULT))
                    Log.e("XRAY_CORE", "Start error: $errorJson")
                }
            } catch (e: Exception) {
                Log.e("XRAY_CORE", "Crash during start", e)
            }
        }.start()
    }

    private fun stopVpn() {
        try {
            LibXray.stopXray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}