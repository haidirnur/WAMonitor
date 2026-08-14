package com.ucok.wamonitor.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ucok.wamonitor.data.AppDatabase
import com.ucok.wamonitor.data.CapturedMessage
import com.ucok.wamonitor.data.FirestoreRepository
import com.ucok.wamonitor.data.Prefs
import kotlinx.coroutines.runBlocking

private const val TAG = "WaMonitorService"
private const val WHATSAPP_PACKAGE = "com.whatsapp"
private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

class WaNotificationListenerService : NotificationListenerService() {

    companion object {
        // Listener untuk menampilkan log di MainActivity (jika app terbuka)
        private var logListener: ((String) -> Unit)? = null

        fun setLogListener(listener: ((String) -> Unit)?) {
            logListener = listener
        }
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
        logListener?.invoke(msg)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        log("🟢 Service terhubung - siap memantau notifikasi")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        log("🔴 Service terputus")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName

        // Hanya proses notifikasi dari WhatsApp
        if (pkg != WHATSAPP_PACKAGE && pkg != WHATSAPP_BUSINESS_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: run {
            log("⚠️ Notifikasi WA tanpa title, skip")
            return
        }

        // Ambil teks pesan — cek bigText dulu (WhatsApp terbaru pakai ini)
        val text = extras.getCharSequence("android.bigText")?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence("android.text")?.toString()
            ?: ""

        log("📬 Notifikasi WA masuk | Dari: \"$title\" | Isi: \"$text\"")

        // Skip notifikasi ringkasan grup
        if (text.isBlank()) {
            log("⏭️ Skip: teks kosong (kemungkinan notif ringkasan)")
            return
        }

        val targets = Prefs.getTargets(applicationContext)
        if (targets.isEmpty()) {
            log("⚠️ Target belum diisi di app! Pesan diabaikan.")
            return
        }

        log("🎯 Target aktif: ${targets.joinToString(", ")}")

        val isMatch = targets.any { target ->
            title.contains(target.trim(), ignoreCase = true)
        }

        if (!isMatch) {
            log("❌ \"$title\" tidak cocok dengan target manapun")
            return
        }

        log("✅ COCOK! Menyimpan pesan dari: $title")

        val message = CapturedMessage(
            senderTitle = title,
            messageText = text,
            timestamp = sbn.postTime
        )

        // Simpan ke Room DB lokal
        Thread {
            try {
                val dao = AppDatabase.getInstance(applicationContext).capturedMessageDao()
                runBlocking { dao.insert(message) }
                log("💾 Tersimpan di database lokal")
            } catch (e: Exception) {
                log("❌ Gagal simpan lokal: ${e.message}")
            }
        }.start()

        // Upload ke Firestore
        FirestoreRepository.uploadMessage(
            message = message,
            onSuccess = { log("☁️ Berhasil upload ke Firestore!") },
            onFailure = { e -> log("❌ Gagal upload Firestore: ${e.message}") }
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
