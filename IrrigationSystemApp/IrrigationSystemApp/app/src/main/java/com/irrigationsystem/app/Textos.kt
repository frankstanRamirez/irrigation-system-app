package com.irrigationsystem.app

fun origenTexto(o: String) = when (o) {
    "manual" -> "Interruptor físico"
    "web" -> "App / panel"
    "auto" -> "Automático (IA)"
    else -> "—"
}

fun motivoTexto(m: String) = when (m) {
    "humedad_alcanzada" -> "Tierra húmeda (90%)"
    "web" -> "Desde la app"
    "boton" -> "Interruptor físico"
    "tiempo_maximo" -> "Tiempo máximo"
    "falla_sensor" -> "Falla del sensor"
    "falla_final_carrera" -> "Falla de recorrido"
    else -> "—"
}

fun errorTexto(e: String) = when (e) {
    "tierra_humeda" -> "La tierra ya está húmeda (90% o más)"
    "falla_sensor" -> "No se puede regar: falla en el sensor"
    "umbral_fuera_de_rango" -> "Umbral fuera de rango"
    else -> "No se pudo completar la acción"
}

fun mmss(seg: Long) = "%02d:%02d".format(seg / 60, seg % 60)
