# 📱 Guía de Compilación DroneScan APK

## 🎯 Resumen del Proyecto
- **Aplicación**: DroneScan v2.12
- **Package**: com.dronescan.msdksample
- **Tipo**: Android App con soporte DJI SDK
- **SDK Mínimo**: Android 26 (8.0)
- **SDK Target**: Android 34

## 🔧 Configuración del Entorno

### Prerrequisitos
```bash
# Java 17 (Configurado ✅)
export JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64

# Android SDK (Requerido)
export ANDROID_HOME=/path/to/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH
```

### Dependencias del Proyecto
- **Android Gradle Plugin**: 8.1.0
- **Kotlin**: 1.7.22
- **DJI SDK**: v5.15.0
- **Target SDK**: 34
- **Min SDK**: 26

## 📦 Proceso de Compilación

### Comando Principal
```bash
cd DroneScanMinimal
./gradlew assembleDebug
```

### APK Generada
- **Ubicación**: `app/build/outputs/apk/debug/`
- **Nombre**: `DroneScan_v2.12-debug_debug.apk`
- **Tamaño Estimado**: ~164MB (incluye DJI SDK)

## 🛠️ Scripts de Compilación Disponibles

### 1. Script de Debug con Celular
```bash
./debug-celular.sh
```
- Compila APK
- Instala en dispositivo conectado via ADB
- Monitorea logs en tiempo real

### 2. Script de Testing BrowserStack
```bash
./browserstack-testing.sh
```
- Compila APK
- Sube a BrowserStack para testing
- Ejecuta en dispositivos reales

### 3. Script de Control de Emulador
```bash
./emulator-control.sh
```
- Gestiona conexión con emulador
- Compila e instala APK
- Monitoreo de aplicación

## 📱 Estructura de la APK

### Características Principales
- **Escáner de códigos QR/Barras**: Utilizando cámara del dispositivo
- **Conectividad USB**: Soporte para dispositivos DJI vía USB Host
- **DJI SDK Integration**: Control y comunicación con drones DJI
- **Exportación CSV**: Función de exportar datos escaneados
- **Gestión de permisos**: USB, cámara, almacenamiento

### Permisos Configurados
- `CAMERA`: Para escaneo de códigos
- `USB_PERMISSION`: Para conectividad con drones
- `READ_EXTERNAL_STORAGE`: Acceso a archivos
- `WRITE_EXTERNAL_STORAGE`: Guardado de datos
- `INTERNET`: Conectividad de red

## 🎯 Funcionalidades Implementadas

### Módulos Principales
1. **DroneScanActivity.kt**: Actividad principal de la aplicación
2. **UsbDroneManager.kt**: Gestión de conexiones USB con drones
3. **PtpPhotoManager.kt**: Manejo de fotos via PTP
4. **CsvExporter.kt**: Exportación de datos a CSV
5. **DroneScanApplication.kt**: Configuración global de la app

### Configuración DJI SDK
- **App Key**: 3196948d4ecce3e531187b11
- **Package Name**: com.dronescan.msdksample
- **Registro automático**: Al inicio de la aplicación

## 🚀 Estado de Compilación

### ✅ Configuraciones Verificadas
- [x] Java 17 configurado correctamente
- [x] Gradle wrapper funcional (v8.2)
- [x] Estructura del proyecto válida
- [x] AndroidManifest.xml completo
- [x] Dependencias definidas en build.gradle

### ❌ Limitaciones Actuales
- [ ] Sin acceso a internet para descargar dependencias
- [ ] Android SDK no instalado en el entorno
- [ ] Repositorios Maven no accesibles

## 💡 Soluciones Recomendadas

### Para Entorno con Internet
```bash
# Instalar Android SDK
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-*.zip
export ANDROID_HOME=/path/to/android-sdk
$ANDROID_HOME/cmdline-tools/bin/sdkmanager "platform-tools" "platforms;android-34"

# Compilar APK
cd DroneScanMinimal
./gradlew assembleDebug
```

### Para Entorno Offline
1. Pre-descargar dependencias en máquina con internet
2. Copiar cache de Gradle (~/.gradle/caches/)
3. Incluir Android SDK completo
4. Ejecutar build offline

### Scripts Automáticos Incluidos
El proyecto incluye varios scripts que automatizan el proceso:

```bash
# Compilación rápida para desarrollo
./debug-celular.sh

# Compilación y testing
./browserstack-testing.sh

# Control de emulador
./emulator-control.sh
```

## 📊 Información del Build

### Versión Actual
- **Version Code**: 32
- **Version Name**: 2.12
- **Build Type**: Debug con sufijo .debug
- **ABI Filters**: armeabi-v7a, x86, arm64-v8a

### Tamaño Estimado de APK
- **Base**: ~5MB (código de la app)
- **DJI SDK**: ~150MB (librerías nativas)
- **Recursos**: ~9MB (imágenes, layouts)
- **Total**: ~164MB

## 🔧 Troubleshooting

### Errores Comunes
1. **"Could not resolve dependencies"**: Verificar conectividad a internet
2. **"Android SDK not found"**: Configurar ANDROID_HOME
3. **"Java version incompatible"**: Usar Java 17

### Logs de Depuración
```bash
# Ver logs detallados del build
./gradlew assembleDebug --debug

# Verificar configuración
./gradlew --version
```

---

**Actualizado**: $(date '+%Y-%m-%d %H:%M')  
**Estado**: Configuración verificada, lista para compilación con conectividad