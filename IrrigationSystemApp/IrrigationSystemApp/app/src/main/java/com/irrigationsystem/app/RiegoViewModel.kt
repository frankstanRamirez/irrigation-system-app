package com.irrigationsystem.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irrigationsystem.app.data.EspApi
import com.irrigationsystem.app.data.EspError
import com.irrigationsystem.app.data.Estado
import com.irrigationsystem.app.data.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Un punto de la gráfica: hora (ms) y humedad (%). */
data class Punto(val t: Long, val h: Int)

data class UiState(
    val estado: Estado? = null,
    val conectado: Boolean = false,
    val cargando: Boolean = true,
    val ip: String = "",
    val historial: List<Punto> = emptyList(),
    val ocupado: Boolean = false,
)

class RiegoViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val api = EspApi { prefs.ip }

    private val _ui = MutableStateFlow(UiState(ip = prefs.ip))
    val ui = _ui.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes = _mensajes.asSharedFlow()

    private var anterior: Estado? = null

    init {
        Notifier.crearCanal(app)
        // Consulta el ESP32 cada 1.5 s, igual que el panel web
        viewModelScope.launch {
            while (isActive) {
                refrescar()
                delay(1500)
            }
        }
    }

    private suspend fun refrescar() {
        try {
            procesar(api.estado())
        } catch (e: Exception) {
            _ui.update { it.copy(conectado = false, cargando = false) }
        }
    }

    private fun procesar(e: Estado) {
        revisarAvisos(e)
        val ahora = System.currentTimeMillis()
        _ui.update { s ->
            val hist = if (e.sensorFalla) s.historial
            else (s.historial + Punto(ahora, e.humedad)).takeLast(400)   // ~10 minutos
            s.copy(estado = e, conectado = true, cargando = false, historial = hist)
        }
        anterior = e
    }

    /** Compara con la lectura anterior y lanza notificaciones cuando algo cambia. */
    private fun revisarAvisos(e: Estado) {
        val a = anterior ?: return
        val ctx = getApplication<Application>()
        if (!a.bomba && e.bomba)
            Notifier.avisar(ctx, 1, "Riego iniciado", "Por ${origenTexto(e.origen)} · humedad ${e.humedad}%")
        if (a.bomba && !e.bomba)
            Notifier.avisar(ctx, 1, "Riego terminado", "${motivoTexto(e.ultimoApagado)} · humedad ${e.humedad}%")
        if (!a.sensorFalla && e.sensorFalla)
            Notifier.avisar(ctx, 2, "Falla en el sensor", "Revisá el sensor de humedad")
        if (!a.fallaRecorrido && e.fallaRecorrido)
            Notifier.avisar(ctx, 3, "Falla en la columna", "La barra no completó su recorrido")
    }

    fun alternarRiego() {
        val e = _ui.value.estado ?: return
        accion {
            when {
                e.bomba && e.origen == "auto" -> api.config(auto = false) // si no, el auto lo reenciende
                e.bomba -> api.apagar()
                else -> api.encender()
            }
        }
    }

    fun cambiarAuto(activo: Boolean) = accion { api.config(auto = activo) }

    fun cambiarUmbral(valor: Int) = accion { api.config(umbral = valor) }

    fun guardarIp(ip: String) {
        prefs.ip = ip.trim()
        anterior = null
        _ui.update { it.copy(ip = prefs.ip, estado = null, historial = emptyList(), conectado = false, cargando = true) }
        viewModelScope.launch { refrescar() }
    }

    private fun accion(bloque: suspend () -> Estado) {
        viewModelScope.launch {
            _ui.update { it.copy(ocupado = true) }
            try {
                procesar(bloque())
            } catch (e: EspError) {
                _mensajes.tryEmit(errorTexto(e.error))
            } catch (e: Exception) {
                _mensajes.tryEmit("Sin conexión con el ESP32")
            } finally {
                _ui.update { it.copy(ocupado = false) }
            }
        }
    }
}
