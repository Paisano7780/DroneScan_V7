#!/bin/bash

echo "🔧 === Script de Debug Rápido para DroneScan ==="

APK_PATH="/workspaces/DroneScan_V7/DroneScanMinimal/app/build/outputs/apk/debug/DroneScan_v2.2-debug_debug.apk"

echo "📱 Verificando emulador..."
if ! adb devices | grep "emulator-5554" | grep "device"; then
    echo "❌ Emulador no encontrado. Iniciando emulador..."
    ~/android-emulator/run-emulator.sh &
    
    echo "⏳ Esperando emulador..."
    timeout=120
    while [ $timeout -gt 0 ] && ! adb devices | grep "emulator-5554" | grep "device"; do
        sleep 3
        timeout=$((timeout - 3))
        echo "   Esperando... ($timeout segundos restantes)"
    done
fi

if adb devices | grep "emulator-5554" | grep "device"; then
    echo "✅ Emulador conectado"
    
    if [ -f "$APK_PATH" ]; then
        echo "📦 Instalando DroneScan v2.2..."
        adb -s emulator-5554 install -r "$APK_PATH"
        
        if [ $? -eq 0 ]; then
            echo "✅ APK instalada exitosamente"
            echo ""
            echo "🚀 Para iniciar la app:"
            echo "   adb -s emulator-5554 shell am start -n com.dronescan.debug/com.dronescan.DroneScanActivity"
            echo ""
            echo "📋 Para ver logs en tiempo real:"
            echo "   adb -s emulator-5554 logcat | grep -E '(DroneScan|AndroidRuntime|FATAL)'"
            echo ""
            echo "🌐 También puedes ver el emulador en: http://localhost:5900"
            echo ""
            echo "¿Quieres iniciar la app ahora? (y/n)"
            read -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                echo "🚀 Iniciando DroneScan..."
                adb -s emulator-5554 shell am start -n com.dronescan.debug/com.dronescan.DroneScanActivity
                echo ""
                echo "📋 Mostrando logs (Ctrl+C para salir):"
                adb -s emulator-5554 logcat | grep -E -i "(dronescan|androidruntime|fatal|error|crash|exception)"
            fi
        else
            echo "❌ Error instalando APK"
        fi
    else
        echo "❌ APK no encontrada en: $APK_PATH"
        echo "💡 Compilar primero: cd /workspaces/DroneScan_V7/DroneScanMinimal && ./gradlew assembleDebug"
    fi
else
    echo "❌ No se pudo conectar al emulador"
fi
