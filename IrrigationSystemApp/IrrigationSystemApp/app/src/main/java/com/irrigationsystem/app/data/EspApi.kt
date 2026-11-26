package com.irrigationsystem.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Error que devuelve el ESP32 (409 tierra_humeda, falla_sensor; 400 umbral_fuera_de_rango). */
class EspError(val codigo: Int, val error: String) : Exception(error)

/** Habla con la API REST del ESP32. [ip] se lee cada vez, así un cambio de IP aplica de una vez. */
class EspApi(private val ip: () -> String) {

    suspend fun estado(): Estado = pedir("GET", "/estado")
    suspend fun encender(): Estado = pedir("POST", "/on")
    suspend fun apagar(): Estado = pedir("POST", "/off")

    suspend fun config(umbral: Int? = null, auto: Boolean? = null): Estado {
        val params = buildList {
            if (umbral != null) add("umbral=$umbral")
            if (auto != null) add("auto=${if (auto) 1 else 0}")
        }.joinToString("&")
        return pedir("POST", "/config?$params")
    }

    private suspend fun pedir(metodo: String, ruta: String): Estado = withContext(Dispatchers.IO) {
        val con = URL("http://${ip()}$ruta").openConnection() as HttpURLConnection
        try {
            con.requestMethod = metodo
            con.connectTimeout = 3000
            con.readTimeout = 3000
            if (metodo == "POST") {
                con.doOutput = true
                con.setFixedLengthStreamingMode(0)
                con.outputStream.close()
            }
            val codigo = con.responseCode
            val stream = if (codigo in 200..299) con.inputStream else con.errorStream
            val cuerpo = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (codigo !in 200..299) {
                val error = runCatching { JSONObject(cuerpo).optString("error") }.getOrDefault("")
                throw EspError(codigo, error)
            }
            Estado.from(JSONObject(cuerpo))
        } finally {
            con.disconnect()
        }
    }
}
