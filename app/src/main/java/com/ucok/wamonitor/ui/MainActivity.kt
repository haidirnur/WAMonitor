package com.ucok.wamonitor.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucok.wamonitor.data.CapturedMessage
import com.ucok.wamonitor.data.FirestoreRepository
import com.ucok.wamonitor.data.Prefs
import com.ucok.wamonitor.databinding.ActivityMainBinding
import com.ucok.wamonitor.service.WaNotificationListenerService
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MessageAdapter
    private val logBuilder = StringBuilder()
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MessageAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Muat target tersimpan
        val saved = Prefs.getTargets(this)
        binding.targetInput.setText(saved.joinToString(", "))

        // Tombol simpan target
        binding.saveTargetButton.setOnClickListener {
            val raw = binding.targetInput.text.toString().trim()
            if (raw.isEmpty()) {
                Toast.makeText(this, "Masukkan minimal satu nama/nomor target", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val targets = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            Prefs.setTargets(this, targets)
            addLog("✅ Target disimpan: ${targets.joinToString(", ")}")
            Toast.makeText(this, "Target disimpan!", Toast.LENGTH_SHORT).show()
            updateStatus()
        }

        // Tombol buka notification access
        binding.openNotificationAccessButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            addLog("📱 Membuka pengaturan Notification Access...")
        }

        // Tombol refresh status
        binding.btnRefreshStatus.setOnClickListener {
            updateStatus()
            addLog("🔄 Status di-refresh")
        }

        // Tombol TEST kirim ke Firestore
        binding.btnTestFirestore.setOnClickListener {
            addLog("🧪 Mengirim data test ke Firestore...")
            val testMsg = CapturedMessage(
                senderTitle = "TEST dari WA Monitor",
                messageText = "Ini pesan test - ${sdf.format(Date())}",
                timestamp = System.currentTimeMillis()
            )
            FirestoreRepository.uploadMessage(
                message = testMsg,
                onSuccess = {
                    runOnUiThread {
                        addLog("✅ BERHASIL kirim ke Firestore! Cek Firebase Console → Firestore → Data")
                        Toast.makeText(this, "Berhasil kirim ke Firestore!", Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { e ->
                    runOnUiThread {
                        addLog("❌ GAGAL kirim ke Firestore: ${e.message}")
                        Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // Tombol hapus log
        binding.btnClearLog.setOnClickListener {
            logBuilder.clear()
            binding.logTextView.text = ""
        }

        // Tombol hapus semua pesan
        binding.clearButton.setOnClickListener {
            FirestoreRepository.clearAll()
            addLog("🗑️ Semua pesan dihapus dari Firestore")
        }

        // Listen pesan dari Firestore
        FirestoreRepository.listenMessages { list ->
            adapter.submitList(list)
            binding.emptyView.visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            if (list.isNotEmpty()) {
                addLog("📨 Firestore update: ${list.size} pesan")
            }
        }

        // Set listener log dari service
        WaNotificationListenerService.setLogListener { msg ->
            runOnUiThread { addLog(msg) }
        }

        updateStatus()
        addLog("▶️ App dimulai")
    }

    override fun onResume() {
        super.onResume()
        val saved = Prefs.getTargets(this)
        binding.targetInput.setText(saved.joinToString(", "))
        updateStatus()
        WaNotificationListenerService.setLogListener { msg ->
            runOnUiThread { addLog(msg) }
        }
    }

    override fun onPause() {
        super.onPause()
        WaNotificationListenerService.setLogListener(null)
    }

    private fun updateStatus() {
        // Cek Notification Access
        val hasAccess = isNotificationServiceEnabled()
        binding.statusNotifAccess.text = if (hasAccess)
            "✅ Notification Access: AKTIF"
        else
            "❌ Notification Access: BELUM DIAKTIFKAN"
        binding.statusNotifAccess.setTextColor(
            if (hasAccess) 0xFF388E3C.toInt() else 0xFFD32F2F.toInt()
        )

        // Cek target
        val targets = Prefs.getTargets(this)
        binding.statusTarget.text = if (targets.isEmpty())
            "❌ Target: belum diisi"
        else
            "✅ Target: ${targets.joinToString(", ")}"
        binding.statusTarget.setTextColor(
            if (targets.isEmpty()) 0xFFD32F2F.toInt() else 0xFF388E3C.toInt()
        )

        // Firebase status — akan diupdate saat test
        binding.statusFirebase.text = "ℹ️ Firebase: Tekan tombol Test untuk cek"
        binding.statusFirebase.setTextColor(0xFF1565C0.toInt())
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { it.trim().startsWith(pkgName) }
    }

    private fun addLog(message: String) {
        val time = sdf.format(Date())
        logBuilder.insert(0, "[$time] $message\n")
        // Batasi 50 baris
        val lines = logBuilder.lines()
        if (lines.size > 50) {
            logBuilder.clear()
            logBuilder.append(lines.take(50).joinToString("\n"))
        }
        binding.logTextView.text = logBuilder.toString()
    }
}
