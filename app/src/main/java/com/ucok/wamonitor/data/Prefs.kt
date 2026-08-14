package com.ucok.wamonitor.data

import android.content.Context

/**
 * Menyimpan "target" yang mau dipantau: bisa nama kontak persis seperti
 * tersimpan di HP (mis. "Budi Kantor") ATAU nomor mentah jika kontak
 * tidak tersimpan (mis. "+62812xxxxxxx"), karena WhatsApp menampilkan
 * salah satu dari itu sebagai judul notifikasi.
 *
 * Boleh diisi beberapa target dipisah koma, semua dicocokkan case-insensitive
 * dan "contains" (bukan exact match) supaya lebih fleksibel.
 */
object Prefs {
    private const val PREF_NAME = "wamonitor_prefs"
    private const val KEY_TARGETS = "targets"

    fun getTargets(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TARGETS, "") ?: ""
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun setTargets(context: Context, targets: List<String>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TARGETS, targets.joinToString(","))
            .apply()
    }
}
