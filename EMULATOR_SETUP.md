# Configuración para Android Emulator Extension

## Pasos para configurar la extensión Android iOS Emulator:

### 1. Abrir Command Palette
- Presionar `Ctrl+Shift+P` (o `Cmd+Shift+P` en Mac)
- Buscar: `emulate` o `android` o `ios`

### 2. Comandos disponibles que deberías ver:
- `Emulate.startAndroidEmulator`
- `Emulate.selectAndroidEmulator` 
- `Emulate.startIOSSimulator`

### 3. Si no aparecen emuladores:
La extensión puede requerir:
- Android Studio instalado
- Emuladores AVD creados previamente
- Variables de entorno configuradas

### 4. Alternativa rápida - Emulador web:
Si la extensión no detecta emuladores locales, podemos usar:
- Appetize.io (emulador web)
- BrowserStack (emulador web)
- Android Studio online

### 5. Configurar APK Testing:
Una vez tengamos emulador funcionando:
1. Instalar APK: `adb install DroneScan_v2.2-debug_debug.apk`
2. Ver logs: `adb logcat | grep -i dronescan`
3. Iniciar app: `adb shell am start -n com.dronescan.debug/com.dronescan.DroneScanActivity`

## Próximos pasos:
1. Abrir Command Palette y buscar comandos de emulador
2. Si no aparecen, configurar Android Studio o usar emulador web
3. Instalar nuestra APK y debuggear
