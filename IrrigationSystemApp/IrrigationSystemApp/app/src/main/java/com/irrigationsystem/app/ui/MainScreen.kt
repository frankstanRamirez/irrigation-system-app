package com.irrigationsystem.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irrigationsystem.app.Punto
import com.irrigationsystem.app.R
import com.irrigationsystem.app.RiegoViewModel
import com.irrigationsystem.app.UiState
import com.irrigationsystem.app.data.Estado
import com.irrigationsystem.app.mmss
import com.irrigationsystem.app.motivoTexto
import com.irrigationsystem.app.origenTexto
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MainScreen(vm: RiegoViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var mostrarIp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.mensajes.collect { snack.showSnackbar(it) } }

    Scaffold(containerColor = Fondo, snackbarHost = { SnackbarHost(snack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Encabezado(ui.conectado) { mostrarIp = true } }
            val e = ui.estado
            if (e == null) {
                item { SinDatos(ui) { mostrarIp = true } }
            } else {
                if (e.sensorFalla) item { Alerta("Falla en el sensor de humedad. Por seguridad no se puede regar hasta revisarlo.", Rojo) }
                if (e.fallaRecorrido) item { Alerta("La barra no completó su recorrido. Revisá el riel y volvé a iniciar el riego.", Rojo) }
                if (e.modoAuto && e.autoBloqueado) item { Alerta("Automático en pausa 1 minuto por seguridad. Se reactiva solo.", Ambar) }
                if (!ui.conectado) item { Alerta("Se perdió la conexión con el ESP32. Mostrando el último dato recibido.", Ambar) }
                item { TarjetaHumedad(e) }
                item { TarjetaRiego(e, ui.ocupado, vm::alternarRiego) }
                item { TarjetaColumna(e) }
                item { TarjetaGrafica(ui.historial, e.umbral, e.humedadStop) }
                item { TarjetaAuto(e, vm::cambiarAuto, vm::cambiarUmbral) }
            }
            item {
                Text(
                    "ESP32 en ${ui.ip}",
                    color = Texto2, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (mostrarIp) {
        DialogoIp(ui.ip, onCerrar = { mostrarIp = false }) {
            vm.guardarIp(it)
            mostrarIp = false
        }
    }
}

// ---------------------------------------------------------------- Piezas comunes

@Composable
private fun Tarjeta(titulo: String, extra: String = "", contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        border = BorderStroke(1.dp, Linea),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(titulo.uppercase(), color = Texto2, fontSize = 12.sp, letterSpacing = 1.sp)
                Text(extra, color = Texto2, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            contenido()
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String, colorValor: Color = Texto2, negrita: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, color = Texto, fontSize = 14.sp)
        Text(valor, color = colorValor, fontSize = 14.sp, fontWeight = if (negrita) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun Alerta(texto: String, color: Color) {
    Box(
        Modifier.fillMaxWidth()
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(texto, color = color, fontSize = 14.sp)
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(5.dp))
        Text(texto, color = Texto2, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------- Encabezado

@Composable
private fun Encabezado(conectado: Boolean, onAjustes: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_stat_drop), contentDescription = null,
            tint = Azul, modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Irrigation System", color = Texto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Riego por columna móvil · ESP32", color = Texto2, fontSize = 12.sp)
        }
        val c = if (conectado) Verde else Rojo
        Row(
            Modifier.background(c.copy(alpha = 0.15f), RoundedCornerShape(99.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).background(c, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(if (conectado) "En línea" else "Sin conexión", color = c, fontSize = 12.sp)
        }
        IconButton(onClick = onAjustes) {
            Icon(Icons.Filled.Settings, contentDescription = "Configurar IP", tint = Texto2)
        }
    }
}

@Composable
private fun SinDatos(ui: UiState, onAjustes: () -> Unit) {
    Tarjeta("Conexión") {
        Text(
            if (ui.cargando) "Conectando con el ESP32 en ${ui.ip}…"
            else "No se pudo conectar con el ESP32 en ${ui.ip}.",
            color = Texto, fontSize = 15.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Revisá que el teléfono esté en la misma red WiFi que el ESP32 y que la IP sea la que muestra el Monitor Serie.",
            color = Texto2, fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onAjustes, modifier = Modifier.fillMaxWidth()) { Text("Cambiar IP") }
    }
}

// ---------------------------------------------------------------- Humedad

private fun DrawScope.marca(pct: Int, color: Color, r: Float) {
    val ang = Math.toRadians(pct * 3.6 - 90.0)
    val p = Offset(center.x + r * cos(ang).toFloat(), center.y + r * sin(ang).toFloat())
    drawCircle(color, 5.dp.toPx(), p)
}

@Composable
private fun TarjetaHumedad(e: Estado) {
    val objetivo = when {
        e.sensorFalla -> Rojo
        e.humedad >= e.humedadStop -> Azul
        e.humedad <= e.umbral -> Ambar
        else -> Verde
    }
    val color by animateColorAsState(objetivo, label = "color")
    val prog by animateFloatAsState(if (e.sensorFalla) 0f else e.humedad / 100f, tween(800), label = "hum")
    val estado = when {
        e.sensorFalla -> "Sensor sin lectura"
        e.humedad >= e.humedadStop -> "Tierra húmeda"
        e.humedad <= e.umbral -> "Tierra seca"
        else -> "Humedad adecuada"
    }

    Tarjeta("Humedad del suelo", "raw ${e.raw}") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val grosor = 16.dp.toPx()
                    val r = (size.minDimension - grosor) / 2f
                    val esquina = Offset(center.x - r, center.y - r)
                    val tam = Size(r * 2, r * 2)
                    drawArc(Linea, 0f, 360f, false, esquina, tam, style = Stroke(grosor))
                    drawArc(color, -90f, 360f * prog, false, esquina, tam, style = Stroke(grosor, cap = StrokeCap.Round))
                    marca(e.umbral, Ambar, r)
                    marca(e.humedadStop, Azul, r)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (e.sensorFalla) "--" else "${e.humedad}%", color = Texto, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text(estado, color = Texto2, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Leyenda(Ambar, "Riega en ${e.umbral}%")
            Spacer(Modifier.width(16.dp))
            Leyenda(Azul, "Apaga en ${e.humedadStop}%")
        }
    }
}

// ---------------------------------------------------------------- Riego

@Composable
private fun TarjetaRiego(e: Estado, ocupado: Boolean, onAlternar: () -> Unit) {
    Tarjeta("Riego", if (e.bomba) mmss(e.segundosRegando) else "") {
        Fila("Bombas", if (e.bomba) "Regando" else "Apagadas", if (e.bomba) Azul else Texto2, negrita = true)
        Fila("Activado por", origenTexto(e.origen))
        Fila("Último apagado", motivoTexto(e.ultimoApagado))
        Spacer(Modifier.height(12.dp))
        val bloqueado = !e.bomba && (e.sensorFalla || e.humedad >= e.humedadStop)
        val texto = when {
            !e.bomba -> "Iniciar riego"
            e.origen == "auto" -> "Detener y desactivar automático"
            else -> "Detener riego"
        }
        Button(
            onClick = onAlternar,
            enabled = !ocupado && !bloqueado,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (e.bomba) Rojo else Verde,
                contentColor = if (e.bomba) Color.White else Color(0xFF06210F),
            ),
        ) {
            Text(texto, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------- Columna móvil

@Composable
private fun TarjetaColumna(e: Estado) {
    val pos = e.posicion.coerceIn(0, 100)
    val estado = when {
        e.regresando -> "Volviendo al inicio"
        e.motor -> if (e.direccion == "derecha") "Avanzando" else "Regresando"
        pos == 0 -> "Estacionada en el inicio"
        else -> "Detenida"
    }
    Tarjeta("Columna móvil", "$pos%") {
        BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)) {
            val carro = 48.dp
            val x by animateDpAsState((maxWidth - carro) * (pos / 100f), tween(900, easing = LinearEasing), label = "pos")
            Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(4.dp).background(Linea, RoundedCornerShape(2.dp)))
            val c = if (e.motor) Verde else Texto2
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = x).size(carro, 28.dp)
                    .background(Linea, RoundedCornerShape(8.dp))
                    .border(2.dp, c, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (!e.motor) "■" else if (e.direccion == "derecha") "→" else "←", color = c, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Fila("Estado", estado)
    }
}

// ---------------------------------------------------------------- Gráfica

@Composable
private fun TarjetaGrafica(historial: List<Punto>, umbral: Int, stop: Int) {
    val minutos = if (historial.size > 1) ((historial.last().t - historial.first().t) / 60000.0) else 0.0
    Tarjeta("Historial de humedad", if (historial.size > 1) "últimos %.1f min".format(minutos) else "") {
        if (historial.size < 2) {
            Text("Recolectando datos…", color = Texto2, fontSize = 14.sp)
        } else {
            Row(Modifier.fillMaxWidth().height(170.dp)) {
                Column(Modifier.fillMaxHeight().padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("100", color = Texto2, fontSize = 10.sp)
                    Text("50", color = Texto2, fontSize = 10.sp)
                    Text("0", color = Texto2, fontSize = 10.sp)
                }
                Canvas(Modifier.weight(1f).fillMaxHeight()) {
                    val w = size.width
                    val h = size.height
                    fun y(v: Int) = h * (1f - v / 100f)
                    listOf(0, 50, 100).forEach {
                        drawLine(Linea, Offset(0f, y(it)), Offset(w, y(it)), 1.dp.toPx())
                    }
                    val guiones = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    drawLine(Ambar, Offset(0f, y(umbral)), Offset(w, y(umbral)), 1.5.dp.toPx(), pathEffect = guiones)
                    drawLine(Azul, Offset(0f, y(stop)), Offset(w, y(stop)), 1.5.dp.toPx(), pathEffect = guiones)
                    val linea = Path()
                    historial.forEachIndexed { i, p ->
                        val x = w * i / (historial.size - 1)
                        if (i == 0) linea.moveTo(x, y(p.h)) else linea.lineTo(x, y(p.h))
                    }
                    drawPath(linea, Verde, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Leyenda(Verde, "Humedad")
                Spacer(Modifier.width(14.dp))
                Leyenda(Ambar, "Umbral")
                Spacer(Modifier.width(14.dp))
                Leyenda(Azul, "Apagado")
            }
        }
    }
}

// ---------------------------------------------------------------- Automatización

@Composable
private fun TarjetaAuto(e: Estado, onAuto: (Boolean) -> Unit, onUmbral: (Int) -> Unit) {
    Tarjeta("Automatización") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Modo automático (IA)", color = Texto, fontSize = 15.sp)
            Switch(
                checked = e.modoAuto,
                onCheckedChange = onAuto,
                colors = SwitchDefaults.colors(checkedTrackColor = Verde, checkedThumbColor = Color.White),
            )
        }
        Spacer(Modifier.height(16.dp))
        var valor by remember { mutableFloatStateOf(e.umbral.toFloat()) }
        var editando by remember { mutableStateOf(false) }
        LaunchedEffect(e.umbral) { if (!editando) valor = e.umbral.toFloat() }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Regar cuando la humedad baje de", color = Texto2, fontSize = 14.sp)
            Text("${valor.roundToInt()}%", color = Ambar, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = valor,
            onValueChange = { editando = true; valor = it },
            onValueChangeFinished = { editando = false; onUmbral(valor.roundToInt()) },
            valueRange = 5f..85f,
            steps = 79,
            colors = SliderDefaults.colors(thumbColor = Ambar, activeTrackColor = Ambar),
        )
        Text(
            "Con el modo automático activo, el sistema riega solo al bajar de este valor y se detiene al llegar a 90%. Para detenerlo por completo, desactivá el switch.",
            color = Texto2, fontSize = 12.sp, lineHeight = 17.sp,
        )
    }
}

// ---------------------------------------------------------------- Diálogo de IP

@Composable
private fun DialogoIp(ipActual: String, onCerrar: () -> Unit, onGuardar: (String) -> Unit) {
    var ip by remember { mutableStateOf(ipActual) }
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Superficie,
        title = { Text("Dirección del ESP32", color = Texto) },
        text = {
            Column {
                Text(
                    "Escribí la IP que muestra el Monitor Serie (IP=...). El teléfono tiene que estar en la misma red WiFi.",
                    color = Texto2, fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    singleLine = true,
                    label = { Text("IP") },
                    placeholder = { Text("10.159.35.147") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(ip) }, enabled = ip.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        },
    )
}
