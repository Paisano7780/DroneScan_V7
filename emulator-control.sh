#!/bin/bash

# Script para gestionar la conexión y control del emulador desde VS Code
# Menú interactivo para todas las operaciones

set -e

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

function show_menu() {
    echo -e "${BLUE}=================================="
    echo -e "     DRONESCAN EMULATOR CONTROL"
    echo -e "==================================${NC}"
    echo ""
    echo "1. 🔌 Conectar al emulador"
    echo "2. 📦 Compilar APK"
    echo "3. ⬆️  Instalar DroneScan APK"
    echo "4. 🚀 Ejecutar app y monitorear logs"
    echo "5. 📋 Solo ver logs actuales"
    echo "6. 📱 Estado del dispositivo"
    echo "7. 🗑️  Desinstalar DroneScan"
    echo "8. ❌ Desconectar"
    echo "9. 🚪 Salir"
    echo ""
    echo -n "Selecciona una opción (1-9): "
}

function connect_emulator() {
    echo -e "${YELLOW}Conectando al emulador...${NC}"
    read -p "IP del emulador (default: 192.168.1.100): " ip
    read -p "Puerto (default: 5555): " port
    ./connect-emulator.sh ${ip:-192.168.1.100} ${port:-5555}
}

function compile_apk() {
    echo -e "${YELLOW}Compilando APK...${NC}"
    cd DroneScanMinimal
    ./gradlew assembleDebug
    cd ..
    echo -e "${GREEN}✅ APK compilado!${NC}"
}

function install_app() {
    echo -e "${YELLOW}Instalando DroneScan...${NC}"
    ./install-dronescan.sh
}

function run_and_monitor() {
    echo -e "${YELLOW}Ejecutando DroneScan y monitoreando...${NC}"
    echo -e "${BLUE}Presiona Ctrl+C para volver al menú${NC}"
    ./run-and-monitor.sh
}

function show_logs() {
    echo -e "${YELLOW}Mostrando logs (Ctrl+C para detener)...${NC}"
    adb logcat -v time "com.dronescan:V" "AndroidRuntime:E" "*:S"
}

function device_status() {
    echo -e "${YELLOW}Estado del dispositivo:${NC}"
    echo ""
    echo "📱 Dispositivos conectados:"
    adb devices -l
    echo ""
    echo "🔋 Información del sistema:"
    adb shell getprop ro.build.version.release
    adb shell getprop ro.product.model
    echo ""
    echo "📦 Apps instaladas con 'dronescan':"
    adb shell pm list packages | grep -i dronescan || echo "   No encontradas"
}

function uninstall_app() {
    echo -e "${YELLOW}Desinstalando DroneScan...${NC}"
    adb uninstall com.dronescan && echo -e "${GREEN}✅ Desinstalado!${NC}" || echo -e "${RED}❌ Error o no estaba instalado${NC}"
}

function disconnect_device() {
    echo -e "${YELLOW}Desconectando dispositivos...${NC}"
    adb disconnect
    echo -e "${GREEN}✅ Desconectado!${NC}"
}

# Hacer scripts ejecutables
chmod +x *.sh

# Menú principal
while true; do
    show_menu
    read choice
    
    case $choice in
        1) connect_emulator ;;
        2) compile_apk ;;
        3) install_app ;;
        4) run_and_monitor ;;
        5) show_logs ;;
        6) device_status ;;
        7) uninstall_app ;;
        8) disconnect_device ;;
        9) echo -e "${GREEN}¡Hasta luego!${NC}"; exit 0 ;;
        *) echo -e "${RED}Opción inválida. Selecciona 1-9.${NC}" ;;
    esac
    
    echo ""
    read -p "Presiona Enter para continuar..."
    clear
done
