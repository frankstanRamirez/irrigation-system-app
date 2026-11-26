# Irrigation System — App Android

App en Kotlin + Jetpack Compose para controlar el sistema de riego por columna móvil (ESP32).

## Qué hace
- Indicador de humedad en vivo, con las marcas del umbral y del 90%.
- Iniciar y detener el riego.
- Ver la columna móvil moviéndose en el riel.
- Gráfica con el historial de humedad de los últimos ~10 minutos.
- Activar o desactivar el modo automático y cambiar el umbral.
- Notificaciones: riego iniciado, riego terminado, falla del sensor y falla de recorrido.
- Pantalla para cambiar la IP del ESP32 (engranaje arriba a la derecha).

## Cómo abrirla y generar el APK
1. Descomprimí el ZIP.
2. En Android Studio: File > Open > elegí la carpeta `IrrigationSystemApp`.
3. Esperá a que termine el "Gradle Sync" (la primera vez descarga cosas, tarda unos minutos).
   Si Android Studio ofrece actualizar el Android Gradle Plugin, podés aceptar.
4. Conectá tu teléfono por USB con la depuración USB activada y dale a Run (el triángulo verde).
   O generá el APK: Build > Build App Bundle(s) / APK(s) > Build APK(s).
   El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Al abrir la app
- Aceptá el permiso de notificaciones.
- El teléfono tiene que estar en la misma red WiFi que el ESP32 (el hotspot).
- Si no conecta, tocá el engranaje y escribí la IP que aparece en el Monitor Serie (IP=...).

## Notas
- La app consulta al ESP32 cada 1.5 segundos, igual que el panel web.
- Las notificaciones llegan mientras la app está abierta o en segundo plano reciente.
  Si Android cierra la app por ahorro de batería, dejan de llegar.
- Usa la misma API del ESP32: GET /estado, POST /on, POST /off y POST /config.
  No hay que cambiar nada en el código del ESP32.
