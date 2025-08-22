#!/bin/bash

echo "🔍 === Monitor del Emulador Android ==="

# Función para mostrar logs del emulador
show_emulator_logs() {
    echo "📋 Logs del emulador:"
    docker logs android-emulator --tail 5
    echo ""
}

# Función para verificar ADB
check_adb() {
    echo "🔗 Verificando ADB..."
    adb devices
    echo ""
}

# Función para instalar y ejecutar APK
install_and_run_apk() {
    APK_PATH="/workspaces/DroneScan_V7/DroneScanMinimal/app/build/outputs/apk/debug/DroneScan_v2.2-debug_debug.apk"
    
    if [ -f "$APK_PATH" ]; then
        echo "📦 Instalando DroneScan v2.2..."
        adb -s emulator-5554 install -r "$APK_PATH"
        
        if [ $? -eq 0 ]; then
            echo "✅ APK instalada exitosamente"
            echo ""
            echo "🚀 Iniciando aplicación..."
            adb -s emulator-5554 shell am start -n com.dronescan.debug/com.dronescan.DroneScanActivity
            echo ""
            echo "📋 Monitoreando logs en tiempo real (Ctrl+C para salir):"
            echo "   Si ves errores aquí, yo los podré leer y ayudar a solucionarlos"
            echo ""
            # Limpiar logcat y mostrar solo logs nuevos
            adb -s emulator-5554 logcat -c
            sleep 2
            adb -s emulator-5554 logcat | grep -E -i "(dronescan|androidruntime|fatal|error|crash|exception|classnotfound)"
        else
            echo "❌ Error instalando APK"
        fi
    else
        echo "❌ APK no encontrada: $APK_PATH"
    fi
}

# Monitorear hasta que el emulador esté listo
echo "⏳ Esperando que el emulador esté listo..."
echo "🌐 Mientras tanto, puedes ver la pantalla en: http://localhost:5900"
echo ""

timeout=300  # 5 minutos máximo
while [ $timeout -gt 0 ]; do
    # Verificar si ADB detecta el emulador
    if adb devices | grep "emulator-5554" | grep "device"; then
        echo "✅ ¡Emulador listo!"
        echo ""
        show_emulator_logs
        check_adb
        
        echo "🎯 ¿Instalar y ejecutar DroneScan ahora? (y/n)"
        read -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            install_and_run_apk
        else
            echo "📋 Para instalar manualmente:"
            echo "   adb -s emulator-5554 install -r $APK_PATH"
            echo "   adb -s emulator-5554 shell am start -n com.dronescan.debug/com.dronescan.DroneScanActivity"
        fi
        exit 0
    fi
    
    # Mostrar progreso cada 30 segundos
    if [ $((timeout % 30)) -eq 0 ]; then
        echo "⏰ Esperando emulador... ($timeout segundos restantes)"
        show_emulator_logs
        check_adb
    fi
    
    sleep 5
    timeout=$((timeout - 5))
done

echo "❌ Timeout: El emulador no se inició en 5 minutos"
echo "📋 Para debugging manual:"
echo "   docker logs android-emulator"
echo "   docker exec -it android-emulator bash"
