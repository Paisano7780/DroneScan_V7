#!/bin/bash

echo "🔍 === Diagnóstico de Espacio para AVD ==="

echo "📊 Espacio disponible:"
df -h | grep -E "(loop4|/workspaces)"

echo ""
echo "📱 Android SDK ocupado:"
du -sh /home/codespace/android-sdk/* 2>/dev/null | sort -hr

echo ""
echo "💾 AVDs existentes:"
ls -la ~/.android/avd/ 2>/dev/null || echo "No hay AVDs"

echo ""
echo "🎯 Soluciones posibles:"
echo "1. Usar dispositivo físico para debug"
echo "2. Usar emulador online (browserstack, etc)"
echo "3. Liberar más espacio eliminando system-images no usadas"
echo "4. Usar ADB WiFi hacia dispositivo físico"

echo ""
echo "🔧 Revisando qué system-images tenemos:"
ls -la /home/codespace/android-sdk/system-images/

echo ""
echo "📏 Tamaño de cada system-image:"
du -sh /home/codespace/android-sdk/system-images/* 2>/dev/null
