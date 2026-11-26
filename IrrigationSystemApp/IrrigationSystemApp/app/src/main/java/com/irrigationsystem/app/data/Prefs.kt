package com.irrigationsystem.app.data

import android.content.Context

/** Guarda la IP del ESP32 en el teléfono. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("riego", Context.MODE_PRIVATE)

    var ip: String
        get() = sp.getString("ip", "10.159.35.147") ?: "10.159.35.147"
        set(valor) = sp.edit().putString("ip", valor).apply()
}
