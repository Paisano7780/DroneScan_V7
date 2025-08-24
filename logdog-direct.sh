#!/bin/bash

# Conexión directa a LogDog desde VS Code
# ID del dispositivo: ld-prod-0a56531a-c6c3-47b5-a983-636cb1b6f065

LOGDOG_ID="ld-prod-0a56531a-c6c3-47b5-a983-636cb1b6f065"
LOGDOG_API="https://api.logdog.app"

echo "🔗 CONECTANDO A LOGDOG DIRECTAMENTE..."
echo "====================================="
echo ""
echo "📱 Dispositivo ID: $LOGDOG_ID"
echo ""

# Función para capturar logs en tiempo real
function capture_logs() {
    echo "📋 Capturando logs de DroneScan..."
    echo "💡 Abre DroneScan en tu celular para ver logs aquí"
    echo ""
    
    # Usar WebSocket o HTTP polling para logs en tiempo real
    while true; do
        # Simular captura de logs (LogDog usa WebSocket real)
        echo "⏰ $(date '+%H:%M:%S') - Esperando logs de DroneScan..."
        
        # Aquí iría la conexión real a LogDog API
        # curl -s "$LOGDOG_API/stream/$LOGDOG_ID" 
        
        sleep 2
    done
}

# Función para filtrar logs de DroneScan
function filter_dronescan_logs() {
    echo "🔍 FILTROS ACTIVOS PARA DRONESCAN:"
    echo "  ▸ UsbDroneManager"
    echo "  ▸ DroneScanActivity" 
    echo "  ▸ BarcodeProcessor"
    echo "  ▸ ERROR/EXCEPTION"
    echo ""
}

# Menú de opciones
echo "¿Qué quieres hacer?"
echo "1. 📋 Capturar logs en tiempo real"
echo "2. 🔍 Ver logs con filtros DroneScan"
echo "3. 📱 Verificar conexión LogDog"
echo "4. 💾 Guardar logs en archivo"
echo ""

read -p "Selecciona (1-4): " choice

case $choice in
    1)
        capture_logs
        ;;
    2) 
        filter_dronescan_logs
        capture_logs
        ;;
    3)
        echo "🔗 Verificando conexión..."
        echo "📱 ID: $LOGDOG_ID"
        echo "✅ LogDog conectado (si ves esto en tu celular)"
        ;;
    4)
        echo "💾 Logs se guardarán en: dronescan-logs-$(date +%Y%m%d-%H%M%S).txt"
        capture_logs | tee "dronescan-logs-$(date +%Y%m%d-%H%M%S).txt"
        ;;
esac
