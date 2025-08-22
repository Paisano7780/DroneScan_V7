#!/bin/bash

# Script para debug rápido cuando el emulador esté listo
echo "🔧 === Debug DroneScan en Emulador VS Code ==="

APK_PATH="/workspaces/DroneScan_V7/DroneScanMinimal/app/build/outputs/apk/debug/DroneScan_v2.2-debug_debug.apk"

echo "📱 Esperando a que el emulador esté disponible..."

# Función para esperar emulador
wait_for_emulator() {
    local timeout=60
    echo "⏳ Esperando conexión del emulador..."
    
    while [ $timeout -gt 0 ]; do
        if adb devices | grep -q "device$"; then
            echo "✅ Emulador conectado!"
            adb devices
            return 0
        fi
        sleep 2
        timeout=$((timeout - 2))
        echo "   Esperando... ($timeout segundos restantes)"
    done
    
    echo "❌ Timeout esperando emulador"
    return 1
}

# Función para instalar y debuggear APK
debug_dronescan() {
    if [ ! -f "$APK_PATH" ]; then
        echo "❌ APK no encontrada: $APK_PATH"
        echo "💡 Compilar primero: cd /workspaces/DroneScan_V7/DroneScanMinimal && ./gradlew assembleDebug"
        return 1
    fi
    
    echo "📦 Instalando DroneScan v2.2..."
    adb install -r "$APK_PATH"
    
    if [ $? -eq 0 ]; then
        echo "✅ APK instalada exitosamente"
        echo ""
        echo "🚀 Iniciando DroneScan..."
        adb shell am start -n com.dronescan.debug/com.dronescan.DroneScanActivity
        
        echo ""
        echo "📋 Logs de la aplicación (Ctrl+C para salir):"
        echo "🔍 Filtrando por errores y DroneScan..."
        adb logcat -c  # Limpiar logs anteriores
        adb logcat | grep -E -i "(dronescan|androidruntime|fatal|crash|exception|error)" --color=always
    else
        echo "❌ Error instalando APK"
        echo "📋 Verificando logs del dispositivo:"
        adb logcat -d | tail -20
    fi
}

# Función principal
main() {
    if wait_for_emulator; then
        debug_dronescan
    else
        echo ""
        echo "💡 Para conectar manualmente cuando el emulador esté listo:"
        echo "   adb connect localhost:5555"
        echo "   adb devices"
        echo ""
        echo "🔄 Luego ejecuta este script nuevamente"
    fi
}

# Solo ejecutar si se llama directamente
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main
fi
