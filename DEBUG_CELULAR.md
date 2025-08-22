# 📱 Debug con Celular - Guía Rápida

## 🚀 Configuración Inicial (Una sola vez)

### 1️⃣ Configurar el celular:
```bash
# En tu celular Android:
# 1. Ve a Configuración > Acerca del teléfono
# 2. Toca 7 veces en 'Número de compilación'
# 3. Ve a Configuración > Opciones de desarrollador
# 4. Activa 'Depuración USB'
# 5. Activa 'Depuración inalámbrica' (Android 11+)
```

### 2️⃣ Conectar por USB (primera vez):
```bash
# Conecta tu celular por USB
adb devices
# Acepta la autorización en el celular
```

### 3️⃣ Cambiar a WiFi:
```bash
adb tcpip 5555
# Desconecta USB
# Busca la IP de tu celular: Configuración > WiFi > [Red] > Avanzado
adb connect [IP_DEL_CELULAR]:5555
```

## 🎯 Debug Diario

### Compilar e instalar:
```bash
./debug-celular.sh
```

### Ver logs en tiempo real:
```bash
./monitor-logs.sh
```

### Solo compilar:
```bash
cd DroneScanMinimal && ./gradlew assembleDebug
```

## ✅ Ventajas del Debug con Celular
- ✅ **Cero espacio** usado en Codespace
- ✅ **Performance real** (no emulado)  
- ✅ **Más rápido** que cualquier emulador
- ✅ **Testing real** en hardware verdadero
- ✅ **Cámara real** para testing de QR
- ✅ **USB real** para testing de comunicación
