#!/bin/bash

echo "📊 === Monitor de Logs DroneScan ==="

export ANDROID_HOME=/home/codespace/android-sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH

echo "🔍 Dispositivos conectados:"
adb devices

echo ""
echo "📱 Iniciando monitoreo de logs..."
echo "   Presiona Ctrl+C para detener"
echo ""

# Limpiar logcat y mostrar solo logs relevantes
adb logcat -c
adb logcat | grep -E "(DroneScan|QRCode|Camera|USB|Error|Exception|AndroidRuntime)" --color=always
