#!/bin/bash

# Script para ejecutar DroneScan y monitorear logs desde VS Code
# Uso: ./run-and-monitor.sh

set -e

PACKAGE_NAME="com.dronescan"
ACTIVITY_NAME="$PACKAGE_NAME.DroneScanActivity"

echo "🚀 Ejecutando DroneScan y monitoreando logs..."

# Verificar que hay dispositivos conectados
if ! adb devices | grep -q "device$"; then
    echo "❌ No hay dispositivos conectados. Ejecuta primero: ./connect-emulator.sh"
    exit 1
fi

# Verificar que la app está instalada
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "❌ DroneScan no está instalada. Ejecuta: ./install-dronescan.sh"
    exit 1
fi

# Limpiar logs previos
echo "🧹 Limpiando logs previos..."
adb logcat -c

# Iniciar la aplicación
echo "📱 Iniciando DroneScan..."
adb shell am start -n "$ACTIVITY_NAME"

# Esperar un momento para que inicie
sleep 2

# Verificar si la actividad se inició correctamente
if adb shell dumpsys activity activities | grep -q "$ACTIVITY_NAME"; then
    echo "✅ DroneScan iniciado correctamente!"
else
    echo "⚠️  La app puede haber tenido problemas al iniciar"
fi

echo ""
echo "📋 Monitoreando logs (presiona Ctrl+C para detener):"
echo "================================================"

# Monitorear logs en tiempo real, filtrando por nuestra app y sistema
adb logcat -v time \
    "$PACKAGE_NAME:V" \
    "AndroidRuntime:E" \
    "System.err:E" \
    "ActivityManager:I" \
    "*:S"
