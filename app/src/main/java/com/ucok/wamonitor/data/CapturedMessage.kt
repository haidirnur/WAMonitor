package com.ucok.wamonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_messages")
data class CapturedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderTitle: String,   // nama/nomor kontak sebagaimana tampil di notifikasi
    val messageText: String,   // isi pesan
    val timestamp: Long        // waktu diterima (epoch millis)
)
