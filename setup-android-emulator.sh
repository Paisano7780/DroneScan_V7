#!/bin/bash

echo "=== Configurando Emulador Android para Debug ===="

# Verificar Docker
if ! command -v docker &> /dev/null; then
    echo "Docker no está disponible"
    exit 1
fi

# Crear directorio para emulador
mkdir -p ~/android-emulator

# Script para ejecutar emulador Android en Docker
cat > ~/android-emulator/run-emulator.sh << 'EOF'
#!/bin/bash

echo "Iniciando emulador Android..."

# Ejecutar emulador Android en Docker con VNC para visualización
docker run -d --privileged \
    --name android-emulator \
    -p 5554:5554 \
    -p 5555:5555 \
    -p 5900:5900 \
    -e EMULATOR_DEVICE="Samsung Galaxy S10" \
    -e WEB_VNC=true \
    budtmo/docker-android:emulator_11.0

echo "Emulador iniciado en puerto 5554"
echo "VNC disponible en puerto 5900"
echo "Esperando que el emulador esté listo..."

# Esperar a que el emulador esté disponible
timeout=60
while [ $timeout -gt 0 ] && ! adb devices | grep "5554" | grep "device"; do
    sleep 2
    timeout=$((timeout - 2))
    echo "Esperando emulador... ($timeout segundos restantes)"
done

if adb devices | grep "5554" | grep "device"; then
    echo "✅ Emulador listo para usar!"
    echo "📱 Dispositivo: $(adb -s emulator-5554 shell getprop ro.product.model 2>/dev/null || echo 'Android Emulator')"
    echo "🔗 Para conectar: adb connect localhost:5554"
else
    echo "❌ Error: El emulador no se inició correctamente"
fi
EOF

chmod +x ~/android-emulator/run-emulator.sh

# Script para instalar APK en emulador
cat > ~/android-emulator/install-apk.sh << 'EOF'
#!/bin/bash

if [ -z "$1" ]; then
    echo "Uso: $0 <ruta-a-apk>"
    exit 1
fi

APK_PATH="$1"

if [ ! -f "$APK_PATH" ]; then
    echo "Error: No se encuentra el archivo APK: $APK_PATH"
    exit 1
fi

echo "Instalando APK en emulador..."
adb -s emulator-5554 install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo "✅ APK instalada exitosamente"
    echo "📱 Para ver logs en tiempo real:"
    echo "   adb -s emulator-5554 logcat | grep -i dronescan"
else
    echo "❌ Error instalando APK"
fi
EOF

chmod +x ~/android-emulator/install-apk.sh

# Script para ver logs
cat > ~/android-emulator/view-logs.sh << 'EOF'
#!/bin/bash

echo "📋 Mostrando logs del emulador (Ctrl+C para salir)..."
echo "🔍 Filtrando por 'dronescan', 'crash', 'error'..."

adb -s emulator-5554 logcat | grep -E -i "(dronescan|androidruntime|fatal|error|crash|exception)"
EOF

chmod +x ~/android-emulator/view-logs.sh

echo ""
echo "✅ Scripts de emulador creados en ~/android-emulator/"
echo ""
echo "📋 Para usar:"
echo "1. Iniciar emulador: ~/android-emulator/run-emulator.sh"
echo "2. Instalar APK: ~/android-emulator/install-apk.sh /ruta/a/tu.apk"
echo "3. Ver logs: ~/android-emulator/view-logs.sh"
echo ""
echo "🌐 También puedes acceder al emulador via web en http://localhost:5900"
