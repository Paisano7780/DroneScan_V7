#!/bin/bash

echo "📱 === Debug DroneScan con Celular vía ADB WiFi ==="

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/home/codespace/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH

echo "🔧 Configuración actual:"
echo "   Java: $(java -version 2>&1 | head -1)"
echo "   ADB: $(adb version | head -1)"
echo ""

# Compilar APK
echo "📦 Compilando APK..."
cd /workspaces/DroneScan_V7/DroneScanMinimal

if ./gradlew assembleDebug; then
    echo "✅ APK compilada exitosamente"
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "📱 APK lista en: $APK_PATH"
        echo ""
        
        echo "🔍 Buscando dispositivos conectados..."
        adb devices -l
        
        DEVICE_COUNT=$(adb devices | grep -c "device$")
        
        if [ $DEVICE_COUNT -gt 0 ]; then
            echo ""
            echo "🚀 Instalando APK en dispositivo..."
            if adb install -r "$APK_PATH"; then
                echo "✅ APK instalada exitosamente"
                echo ""
                echo "📋 Para ver logs en tiempo real:"
                echo "   adb logcat | grep -E '(DroneScan|AndroidRuntime|System.err)'"
                echo ""
                echo "🎯 Para reinstalar después de cambios:"
                echo "   ./debug-celular.sh"
            else
                echo "❌ Error instalando APK"
            fi
        else
            echo ""
            echo "📋 INSTRUCCIONES DE CONEXIÓN:"
            echo "1. Conecta tu celular por USB"
            echo "2. Acepta la depuración USB"
            echo "3. Ejecuta: adb tcpip 5555"
            echo "4. Desconecta USB"
            echo "5. Ejecuta: adb connect [IP_CELULAR]:5555"
            echo "6. Vuelve a ejecutar este script"
        fi
    else
        echo "❌ No se encontró la APK compilada"
    fi
else
    echo "❌ Error compilando APK"
    exit 1
fi
