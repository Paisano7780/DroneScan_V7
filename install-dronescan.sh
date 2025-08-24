#!/bin/bash

# Script para instalar DroneScan APK en el emulador desde VS Code
# Uso: ./install-dronescan.sh [archivo_apk]

set -e

# Configuración
APK_FILE=${1:-"DroneScanMinimal/app/build/outputs/apk/debug/app-debug.apk"}
PACKAGE_NAME="com.dronescan"

echo "📦 Instalando DroneScan APK..."

# Verificar que hay dispositivos conectados
if ! adb devices | grep -q "device$"; then
    echo "❌ No hay dispositivos conectados. Ejecuta primero: ./connect-emulator.sh"
    exit 1
fi

# Verificar que el APK existe
if [ ! -f "$APK_FILE" ]; then
    echo "❌ APK no encontrado: $APK_FILE"
    echo "💡 Compila primero con: ./gradlew assembleDebug"
    exit 1
fi

echo "📱 Instalando APK: $APK_FILE"

# Desinstalar versión previa si existe
echo "🗑️  Desinstalando versión previa..."
adb uninstall $PACKAGE_NAME > /dev/null 2>&1 || echo "   (No había versión previa)"

# Instalar nueva versión
echo "⬆️  Instalando nueva versión..."
if adb install "$APK_FILE"; then
    echo "✅ APK instalado exitosamente!"
    
    # Mostrar información de la app instalada
    echo ""
    echo "📋 Información de la app:"
    adb shell pm list packages | grep dronescan || echo "   Package: $PACKAGE_NAME"
    
else
    echo "❌ Error al instalar APK"
    exit 1
fi

echo ""
echo "🚀 Para ejecutar la app:"
echo "   adb shell am start -n $PACKAGE_NAME/.DroneScanActivity"
