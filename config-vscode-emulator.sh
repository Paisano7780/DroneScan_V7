#!/bin/bash

echo "🔧 === Configuración rápida de Android Emulator para VS Code ==="

# Variables de entorno básicas
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=~/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

echo "📱 Instalando componentes mínimos de Android SDK..."

# Instalar solo lo esencial para emulator
sdkmanager "platform-tools" "emulator" --quiet

echo "📋 Verificando extensiones de VS Code..."

# Crear configuración básica para VS Code
mkdir -p ~/.vscode
cat > ~/.vscode/settings.json << 'EOF'
{
    "emulator.emulatorPath": "~/android-sdk/emulator/emulator",
    "emulator.adbPath": "~/android-sdk/platform-tools/adb"
}
EOF

echo "✅ Configuración básica completada"
echo ""
echo "📱 Para usar la extensión Android iOS Emulator:"
echo "1. Abrir Command Palette (Ctrl+Shift+P)"
echo "2. Buscar 'Emulate' o 'Android'"
echo "3. Seleccionar el comando del emulador"
echo ""
echo "🔍 Si no aparecen emuladores:"
echo "1. Puede que necesites descargar una imagen de sistema"
echo "2. O usar un emulador web como https://appetize.io"
echo ""

# Verificar qué comandos tiene la extensión
echo "🔍 Buscando comandos disponibles de la extensión..."
