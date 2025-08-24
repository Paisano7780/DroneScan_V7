#!/bin/bash

# Script para usar LogDog con DroneScan desde VS Code
# Alternativa profesional a capturas de pantalla

echo "🔍 LOGDOG SETUP PARA DRONESCAN"
echo "================================"
echo ""

echo "📱 OPCIÓN 1: LogDog (App de Play Store)"
echo "▸ Instala: https://play.store/apps/details?id=com.logdog"
echo "▸ Conecta por WiFi a tu Codespace"
echo "▸ Logs en tiempo real en tu PC"
echo ""

echo "📱 OPCIÓN 2: ADB WiFi + LogCat directo"
echo "▸ Conecta celular por WiFi"
echo "▸ Logs directos en VS Code terminal"
echo ""

echo "¿Cuál prefieres?"
echo "1) Configurar LogDog WiFi"
echo "2) Configurar ADB WiFi directo"
echo "3) Ver ambas opciones"
echo ""

read -p "Selecciona (1-3): " choice

case $choice in
    1)
        echo ""
        echo "🚀 CONFIGURACIÓN LOGDOG:"
        echo "========================"
        echo ""
        echo "1. 📲 Instala LogDog en tu celular"
        echo "2. 🌐 Conecta celular y Codespace a la misma WiFi"
        echo "3. 📡 Abre LogDog > Settings > Network"
        echo "4. 🔗 Obtén IP del celular"
        echo "5. 💻 Conéctate desde VS Code:"
        echo ""
        echo "   adb connect [IP_CELULAR]:5555"
        echo "   adb logcat -s DroneScan UsbDroneManager"
        echo ""
        ;;
        
    2)
        echo ""
        echo "🚀 CONFIGURACIÓN ADB WIFI:"
        echo "=========================="
        echo ""
        echo "1. 📱 Activa 'Depuración WiFi' en tu celular"
        echo "2. 🔗 Conecta por WiFi:"
        echo ""
        echo "   # En tu celular, ve a Configuración > Desarrollador"
        echo "   # Activa 'Depuración inalámbrica'"
        echo "   # Anota IP y puerto que aparece"
        echo ""
        echo "3. 💻 Desde VS Code:"
        echo ""
        echo "   adb pair [IP]:[PUERTO] [CODIGO]"
        echo "   adb connect [IP]:[PUERTO]"
        echo "   adb logcat | grep -E 'DroneScan|UsbDroneManager|ERROR'"
        echo ""
        ;;
        
    3)
        echo ""
        echo "📊 COMPARACIÓN:"
        echo "==============="
        echo ""
        echo "LogDog App:"
        echo "✅ Interface gráfica"
        echo "✅ Filtros visuales"
        echo "✅ Fácil de usar"
        echo "❌ App adicional"
        echo ""
        echo "ADB WiFi Directo:"
        echo "✅ Integrado en VS Code"
        echo "✅ Automatable con scripts"
        echo "✅ Logs en terminal"
        echo "❌ Requiere más setup"
        echo ""
        ;;
esac

echo ""
echo "💡 TIP: Para DroneScan usa estos filtros:"
echo "   adb logcat -s DroneScan:V UsbDroneManager:V *:E"
echo ""
echo "🔧 Script listo. ¿Configuramos WiFi debugging?"
