#!/bin/bash

# Script para instalar DroneScanMinimal en dispositivo Android
# Uso: ./install_app.sh

echo "🚀 DroneScan USB Native - Instalador"
echo "====================================="

# Verificar que existe el APK
APK_PATH="/workspaces/DroneScan_V7/DroneScanMinimal/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: No se encontró el APK en $APK_PATH"
    echo "🔧 Compila primero la aplicación con: ./gradlew assembleDebug"
    exit 1
fi

echo "✅ APK encontrado: $(basename $APK_PATH)"
echo "📱 Tamaño: $(ls -lh $APK_PATH | awk '{print $5}')"
echo ""

# Verificar conexión ADB
echo "🔍 Verificando dispositivos conectados..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ No hay dispositivos Android conectados"
    echo ""
    echo "📱 Para conectar tu celular:"
    echo "1. Activa 'Opciones de desarrollador' en Android"
    echo "2. Activa 'Depuración USB'"
    echo "3. Conecta el celular con cable USB"
    echo "4. Acepta el diálogo de confianza en el celular"
    echo "5. Ejecuta nuevamente este script"
    echo ""
    echo "🔧 Para verificar manualmente: adb devices"
    exit 1
fi

echo "✅ Dispositivos conectados: $DEVICES"
adb devices
echo ""

# Instalar APK
echo "📦 Instalando DroneScan USB Native..."
echo "⚠️  Si ya está instalada, se actualizará automáticamente"
echo ""

if adb install -r "$APK_PATH"; then
    echo ""
    echo "🎉 ¡Instalación exitosa!"
    echo "📱 DroneScan USB Native está lista para usar"
    echo ""
    echo "🚀 Próximos pasos:"
    echo "1. Busca 'DroneScan' en el menú de aplicaciones"
    echo "2. Abre la aplicación"
    echo "3. Concede permisos de almacenamiento y USB"
    echo "4. Conecta tu drone DJI vía USB"
    echo "5. ¡Escanea códigos de barras en fotos!"
    echo ""
    echo "🔧 Para logs en tiempo real: adb logcat | grep DroneScan"
else
    echo ""
    echo "❌ Error durante la instalación"
    echo "🔧 Soluciones:"
    echo "1. Verifica que el USB debugging esté activo"
    echo "2. Acepta permisos en el dispositivo"
    echo "3. Intenta desinstalar la versión anterior manualmente"
    echo "4. Ejecuta: adb uninstall com.dronescan.msdksample"
fi

echo ""
echo "📊 Información adicional:"
echo "📁 APK: $APK_PATH"
echo "📱 Package: com.dronescan.msdksample"
echo "🔧 ADB: $(adb version | head -1)"
