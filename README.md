
# DroneScan_V7

## Requisitos Java

**⚠️ IMPORTANTE: Esta aplicación requiere Java 17 para compilar.**

Ver [JAVA_VERSION_REQUIREMENTS.md](JAVA_VERSION_REQUIREMENTS.md) para detalles completos sobre requisitos y configuración.

```bash
# Verificar versión de Java
java -version
# Debe mostrar Java 17 o superior

# Compilar APK
./debug-celular.sh
```

## Descripción — Descripción Técnica y Guía de Reconstrucción

## Descripción General
DroneScan_V7 es una aplicación Android para la gestión y escaneo de fotos tomadas por drones DJI, modernizada y optimizada para funcionar con el SDK DJI Mobile V5.15.0. El proyecto ha sido migrado completamente a Kotlin, eliminando duplicados y dependencias Java innecesarias, y asegurando compatibilidad con dispositivos modernos como el DJI RM330.

## ✨ ÚLTIMA ACTUALIZACIÓN - v2.11 (Agosto 2024)

### 🔧 Cambios Críticos Implementados:

#### **📱 Registro DJI SDK**
- **App Key configurado**: `5c6f77b3b3c80d95f76c8584` (registrado en DJI Developer)
- **Package Name corregido**: `com.dronescan.msdksample` (coincide con registro DJI)
- **Registro automático en DroneScanApplication.kt**: Inicialización del SDK al arrancar la app
- **Dependencies DJI SDK agregadas**: v5.15.0 con repositorio Maven oficial

#### **🔌 Detección USB Host/Device Robusta**
- **Patrón Android-Bridge-App implementado**: Verificación exacta del primer accesorio DJI
- **Timer automático con RxJava**: Observable.timer cada 1 segundo para monitoreo continuo
- **Application class con lifecycle callbacks**: DroneScanApplication.kt maneja eventos del sistema
- **onNewIntent en Activity**: Manejo de eventos USB cuando la app está en background
- **Doble verificación USB**: Accessory pattern (DJI oficial) + Device pattern (fallback para RM330)
- **Diagnóstico de modo USB**: sys.usb.config y sys.usb.state logging
- **VendorID ampliado**: Soporte para múltiples VendorIDs posibles del RM330

#### **📋 Logging Avanzado**
- **Límite aumentado**: De 100 a 1000 logs para mejor debugging
- **Inicialización en Application**: DebugLogger.init() llamado al arrancar
- **Diagnóstico USB completo**: deviceList, accessoryList, permisos, VendorIDs, ProductIDs
- **Modo USB debugging**: Detección de host/device mode
- **Filtros XML actualizados**: UsbAccessory y UsbDevice filters en device_filter.xml

#### **🔐 Permisos y Manifiesto**
- **USB Host feature**: android.hardware.usb.host declarado
- **FileProvider authorities corregido**: com.dronescan.msdksample.fileprovider
- **Meta-data DJI**: App Key agregado al AndroidManifest.xml
- **Intent filters USB**: ACTION_USB_DEVICE_ATTACHED y ACTION_USB_ACCESSORY_ATTACHED

### 🔄 Evolución del Proyecto:

#### **v2.7-v2.9**: Implementación Bridge Pattern
- Análisis exhaustivo del Android-Bridge-App de la comunidad DJI
- Implementación de RxJava/RxAndroid para timer automático
- Application class con lifecycle callbacks
- onNewIntent para eventos USB en background
- Verificación exacta del primer accesorio DJI

#### **v2.10**: Corrección de Permisos y Modo USB
- Eliminación de verificación de modelo de celular
- Diagnóstico de modo USB (host vs device)
- Logging de sys.usb.config y sys.usb.state
- Mejora en detección de dispositivos USB

#### **v2.11**: Registro DJI SDK y Correcciones Finales
- App Key real configurado y package name corregido
- Función checkForDJIAccessory() reparada (eliminada duplicación)
- FileProvider authorities corregido
- Compilación exitosa con todos los cambios

## Principales acciones realizadas
- **Migración total a Kotlin**: Todo el código relevante fue convertido a Kotlin, eliminando clases y fragments Java redundantes.
- **Integración y limpieza de Gradle**: Se revisaron y ajustaron los archivos `build.gradle` y `settings.gradle` para asegurar dependencias mínimas y build limpio.
- **Compatibilidad DJI SDK V5.15.0**: Se adaptó la lógica de obtención y descarga de la última foto usando `MediaManager` y filtrado robusto por nombre de archivo, descartando APIs no públicas o inestables.
- **Implementación de patrones de la comunidad**: Android-Bridge-App pattern para detección USB robusta.
- **Registro DJI SDK**: App Key y Package Name configurados correctamente.
- **Eliminación de código basura**: Se identificaron y eliminaron módulos, utilidades y recursos no usados, y se documentó una lista de limpieza adicional en `LIMPIEZA_SUGERIDA.md`.
- **Centralización de logs y errores**: Se implementó un logger global (`DebugLogger`) que registra cualquier excepción o crash en memoria para debugging, con límite aumentado a 1000 logs.
- **Permisos y compatibilidad RM330**: Se ajustó la gestión de permisos USB y modo host/device para funcionamiento en DJI RC.

## Lógica principal
1. **Inicialización**: DroneScanApplication.kt inicializa el SDK DJI y el logger al arrancar la app.
2. **Detección USB**: UsbDroneManager.kt usa timer automático para detectar dispositivos DJI por accessory/device pattern.
3. **Escaneo y descarga de fotos**: Al conectar el dron, la app escanea la SD, filtra las fotos JPEG, identifica la más reciente por nombre correlativo y la descarga localmente.
4. **Procesamiento y escaneo de código de barras**: Tras la descarga, la imagen se pasa a un módulo de escaneo de códigos de barras y, si es exitoso, se genera un CSV con los datos.
5. **Gestión de errores**: Cualquier error crítico se muestra en pantalla y se registra en el log interno accesible desde el botón "Copiar Logs".

## 🔧 Estado Actual y Problemas Conocidos

### ✅ Funcionando:
- ✅ Compilación exitosa (APK v2.11 generada)
- ✅ Registro DJI SDK implementado
- ✅ Package name correcto (com.dronescan.msdksample)
- ✅ App Key configurado (5c6f77b3b3c80d95f76c8584)
- ✅ Timer automático RxJava implementado
- ✅ Logging interno robusto (1000 logs, copiar/pegar)
- ✅ Application class con lifecycle callbacks
- ✅ onNewIntent para eventos USB
- ✅ Diagnóstico de modo USB completo

### ⚠️ En Investigación:
- ⚠️ **RM330 no aparece como dispositivo USB**: deviceList vacío, host: false
- ⚠️ **Celular en modo device**: sys.usb.config=peripheral, state=configured
- ⚠️ **Posible cable/configuración**: Verificar cable USB-C, modo de conexión RM330

### 💡 Próximos Pasos:
1. **Probar en hardware**: Instalar APK v2.11 en RM330 y verificar detección
2. **Verificar cable USB**: Asegurar que el cable soporte data + power
3. **Modo RM330**: Revisar configuración de conexión USB en el RM330
4. **Debugging avanzado**: Usar adb shell para verificar dispositivos USB del sistema

## Recomendaciones para reconstrucción desde cero
1. **Estructura mínima**: Mantener solo los módulos y clases estrictamente necesarios para la lógica de escaneo, descarga y procesamiento de fotos.
2. **Integrar DJI SDK V5.15.0**: Seguir la lógica de MediaManager y registrar App Key desde el inicio.
3. **Implementar Bridge Pattern**: Usar el patrón exacto del Android-Bridge-App para detección USB.
4. **Application class obligatoria**: DroneScanApplication.kt con lifecycle callbacks y timer RxJava.
5. **Centralizar logs y manejo de errores**: Implementar un logger global desde el inicio.
6. **Permisos USB y compatibilidad**: Priorizar permisos USB Host y pruebas en dispositivos DJI RC.
7. **Package name crucial**: Debe coincidir exactamente con el registro DJI Developer.

## Recursos clave
- `DroneScanApplication.kt`: Application class con registro DJI SDK y lifecycle
- `DroneScanActivity.kt`: Lógica principal de escaneo y descarga + onNewIntent
- `UsbDroneManager.kt`: Detección USB robusta con Bridge pattern y timer RxJava
- `DebugLogger.kt`: Logger interno con 1000 logs y función copiar
- `AndroidManifest.xml`: App Key, permisos USB, intent filters
- `build.gradle`: Dependencias DJI SDK, RxJava, package name correcto
- `device_filter.xml`: Filtros USB para accessory y device

---
Este README sirve como referencia técnica para reconstruir el proyecto en caso de desastre, asegurando que los aprendizajes y buenas prácticas implementadas no se pierdan.
