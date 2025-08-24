#!/bin/bash

# Script para conectar al emulador local desde VS Code via ADB
# Uso: ./connect-emulator.sh [IP_EMULADOR] [PUERTO]

set -e

# Configuración por defecto - ACTUALIZA CON TU IP REAL
DEFAULT_IP="192.168.1.100"  # ⚠️  CAMBIAR por la IP real de tu PC
DEFAULT_PORT="5555"

# Leer configuración si existe
if [ -f "emulator-config.conf" ]; then
    source emulator-config.conf
    DEFAULT_IP=${EMULATOR_IP:-$DEFAULT_IP}
    DEFAULT_PORT=${EMULATOR_PORT:-$DEFAULT_PORT}
fi

# Usar parámetros o valores por defecto
EMULATOR_IP=${1:-$DEFAULT_IP}
EMULATOR_PORT=${2:-$DEFAULT_PORT}

echo "🔌 Conectando al emulador en ${EMULATOR_IP}:${EMULATOR_PORT}..."

# Desconectar conexiones previas
adb disconnect > /dev/null 2>&1 || true

# Conectar al emulador
if adb connect ${EMULATOR_IP}:${EMULATOR_PORT}; then
    echo "✅ Conectado exitosamente!"
    
    # Verificar dispositivos conectados
    echo ""
    echo "📱 Dispositivos conectados:"
    adb devices -l
    
    # Verificar que el dispositivo esté listo
    echo ""
    echo "🔍 Verificando estado del dispositivo..."
    adb wait-for-device
    echo "✅ Dispositivo listo para comandos"
    
else
    echo "❌ Error al conectar. Verifica que:"
    echo "   1. El emulador esté ejecutándose en tu PC local"
    echo "   2. La IP ${EMULATOR_IP} sea correcta"
    echo "   3. El puerto ${EMULATOR_PORT} esté disponible"
    echo "   4. No haya firewall bloqueando la conexión"
    exit 1
fi
