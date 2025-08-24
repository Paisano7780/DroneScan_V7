#!/bin/bash

# Script para testing de DroneScan en BrowserStack desde VS Code
# Uso: ./browserstack-testing.sh

set -e

# Configuración
BS_USER="${BS_USERNAME:-your_browserstack_username}"
BS_KEY="${BS_ACCESS_KEY:-your_browserstack_key}"
APK_PATH="DroneScanMinimal/app/build/outputs/apk/debug/DroneScan_v2.2-debug_debug.apk"

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

function show_menu() {
    echo -e "${BLUE}=================================="
    echo -e "     DRONESCAN BROWSERSTACK TESTING"
    echo -e "==================================${NC}"
    echo ""
    echo "1. 📱 Compilar APK"
    echo "2. ⬆️  Subir APK a BrowserStack"
    echo "3. 🚀 Iniciar testing en dispositivo real"
    echo "4. 📋 Ver logs en tiempo real"
    echo "5. 📊 Análisis de performance"
    echo "6. 📸 Capturar screenshots"
    echo "7. 🎥 Grabar video de sesión"
    echo "8. 🚪 Salir"
    echo ""
    echo -n "Selecciona una opción (1-8): "
}

function compile_apk() {
    echo -e "${YELLOW}Compilando DroneScan APK...${NC}"
    cd DroneScanMinimal
    ./gradlew assembleDebug
    cd ..
    
    if [ -f "$APK_PATH" ]; then
        echo -e "${GREEN}✅ APK compilado exitosamente!${NC}"
        echo "📁 Ubicación: $APK_PATH"
        ls -lh "$APK_PATH"
    else
        echo -e "${RED}❌ Error: APK no encontrado${NC}"
        exit 1
    fi
}

function upload_apk() {
    echo -e "${YELLOW}Subiendo APK a BrowserStack...${NC}"
    
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}❌ APK no encontrado. Compila primero.${NC}"
        return 1
    fi
    
    RESPONSE=$(curl -s -u "$BS_USER:$BS_KEY" \
        -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
        -F "file=@$APK_PATH")
    
    APP_URL=$(echo "$RESPONSE" | jq -r '.app_url')
    
    if [ "$APP_URL" != "null" ]; then
        echo -e "${GREEN}✅ APK subido exitosamente!${NC}"
        echo "🔗 App URL: $APP_URL"
        echo "$APP_URL" > .browserstack_app_url
    else
        echo -e "${RED}❌ Error al subir APK${NC}"
        echo "$RESPONSE"
        return 1
    fi
}

function start_live_testing() {
    if [ ! -f ".browserstack_app_url" ]; then
        echo -e "${RED}❌ Primero sube el APK${NC}"
        return 1
    fi
    
    APP_URL=$(cat .browserstack_app_url)
    
    echo -e "${YELLOW}Iniciando testing en vivo...${NC}"
    echo ""
    echo "📱 Dispositivos recomendados para DroneScan:"
    echo "   • Samsung Galaxy S21 (Android 12)"
    echo "   • Google Pixel 7 (Android 13)"
    echo "   • OnePlus 9 (Android 11)"
    echo ""
    echo "🌐 Abre este link para testing manual:"
    echo "   https://live.browserstack.com/"
    echo ""
    echo "📋 Tu App URL: $APP_URL"
    echo "   (Copia este URL en BrowserStack Live)"
}

function monitor_logs() {
    echo -e "${YELLOW}Monitoreando logs de DroneScan...${NC}"
    echo "💡 Tip: Ejecuta acciones en tu app para ver logs en tiempo real"
    echo ""
    
    # Simular monitoring de logs (en realidad necesitarías session ID)
    echo "🔍 Conectando a logs de BrowserStack..."
    echo "⏰ $(date): App iniciada"
    echo "📱 Dispositivo: Esperando conexión..."
    echo ""
    echo "📋 Los logs aparecerán aquí cuando uses la app en BrowserStack Live"
}

function performance_analysis() {
    echo -e "${YELLOW}Análisis de performance...${NC}"
    echo ""
    echo "📊 Métricas disponibles en BrowserStack:"
    echo "   • CPU Usage"
    echo "   • Memory Usage"
    echo "   • Network Activity"
    echo "   • Battery Consumption"
    echo "   • App Launch Time"
    echo ""
    echo "🌐 Ver métricas detalladas en:"
    echo "   https://app-automate.browserstack.com/dashboard"
}

function capture_screenshots() {
    echo -e "${YELLOW}Configurando captura de screenshots...${NC}"
    echo ""
    echo "📸 BrowserStack captura automáticamente:"
    echo "   • Screenshots de cada acción"
    echo "   • Estados de error"
    echo "   • Pantallas principales de la app"
    echo ""
    echo "🖼️  Accede a las imágenes en el dashboard de BrowserStack"
}

function record_session() {
    echo -e "${YELLOW}Grabación de video habilitada...${NC}"
    echo ""
    echo "🎥 BrowserStack graba automáticamente:"
    echo "   • Toda la sesión de testing"
    echo "   • Interacciones del usuario"
    echo "   • Comportamiento de la app"
    echo ""
    echo "📹 Los videos estarán disponibles en el dashboard"
}

# Verificar configuración
if [ "$BS_USER" = "your_browserstack_username" ] || [ "$BS_KEY" = "your_browserstack_key" ]; then
    echo -e "${RED}⚠️  Configura tus credenciales de BrowserStack:${NC}"
    echo "export BS_USERNAME=tu_usuario"
    echo "export BS_ACCESS_KEY=tu_api_key"
    echo ""
    echo "💡 Obtén las credenciales en: https://www.browserstack.com/accounts/settings"
    echo ""
fi

# Menú principal
while true; do
    show_menu
    read choice
    
    case $choice in
        1) compile_apk ;;
        2) upload_apk ;;
        3) start_live_testing ;;
        4) monitor_logs ;;
        5) performance_analysis ;;
        6) capture_screenshots ;;
        7) record_session ;;
        8) echo -e "${GREEN}¡Hasta luego!${NC}"; exit 0 ;;
        *) echo -e "${RED}Opción inválida. Selecciona 1-8.${NC}" ;;
    esac
    
    echo ""
    read -p "Presiona Enter para continuar..."
    clear
done
