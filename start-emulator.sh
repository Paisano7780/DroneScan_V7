#!/bin/bash

echo "🚀 === Iniciando Emulador DroneScan_Test ==="

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/home/codespace/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH

echo "📱 Iniciando emulador en segundo plano..."
/home/codespace/android-sdk/emulator/emulator -avd DroneScan_Test -no-audio -no-window &

echo "⏳ Esperando que el emulador esté listo..."
timeout=120
while [ $timeout -gt 0 ] && ! adb devices | grep "emulator-" | grep "device"; do
    sleep 3
    timeout=$((timeout - 3))
    echo "   Esperando... ($timeout segundos restantes)"
done

if adb devices | grep "emulator-" | grep "device"; then
    echo "✅ Emulador listo!"
    echo "📱 Dispositivos conectados:"
    adb devices
    
    echo ""
    echo "🎯 Para instalar y debuggear DroneScan, ejecuta:"
    echo "   /workspaces/DroneScan_V7/debug-emulator.sh"
else
    echo "❌ Timeout esperando emulador"
fi
