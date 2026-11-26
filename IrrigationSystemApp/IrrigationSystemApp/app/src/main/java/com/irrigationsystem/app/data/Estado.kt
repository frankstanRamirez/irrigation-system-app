package com.irrigationsystem.app.data

import org.json.JSONObject

/** Copia exacta de lo que devuelve GET /estado en el ESP32. */
data class Estado(
    val humedad: Int,
    val raw: Int,
    val sensorFalla: Boolean,
    val bomba: Boolean,
    val motor: Boolean,
    val fallaRecorrido: Boolean,
    val direccion: String,
    val origen: String,
    val modoAuto: Boolean,
    val autoBloqueado: Boolean,
    val umbral: Int,
    val humedadStop: Int,
    val segundosRegando: Long,
    val ultimoApagado: String,
    val posicion: Int,
    val regresando: Boolean,
) {
    companion object {
        fun from(j: JSONObject) = Estado(
            humedad = j.optInt("humedad"),
            raw = j.optInt("raw"),
            sensorFalla = j.optBoolean("sensorFalla"),
            bomba = j.optBoolean("bomba"),
            motor = j.optBoolean("motor"),
            fallaRecorrido = j.optBoolean("fallaRecorrido"),
            direccion = j.optString("direccion", "derecha"),
            origen = j.optString("origen", "ninguno"),
            modoAuto = j.optBoolean("modoAuto"),
            autoBloqueado = j.optBoolean("autoBloqueado"),
            umbral = j.optInt("umbral", 35),
            humedadStop = j.optInt("humedadStop", 90),
            segundosRegando = j.optLong("segundosRegando"),
            ultimoApagado = j.optString("ultimoApagado", "ninguno"),
            posicion = j.optInt("posicion"),
            regresando = j.optBoolean("regresando"),
        )
    }
}
