# ⚙️ CONFIGURACIÓN PARA LA EXTENSIÓN ANDROID iOS EMULATOR

## 📋 Copia estos valores exactamente en la configuración de la extensión:

### Rutas principales del emulador:

**Emulator Path Linux:**
```
/home/codespace/android-sdk/emulator/emulator
```

**Emulator Path (general):**
```
/home/codespace/android-sdk/emulator
```

**ADB Path:**
```
/home/codespace/android-sdk/platform-tools/adb
```

### Variables de entorno adicionales:

**ANDROID_HOME:**
```
/home/codespace/android-sdk
```

---

## 🎯 PASOS PARA CONFIGURAR:

1. **Ve a Extensions → Docked Android iOS Emulator → Settings (⚙️)**

2. **Busca SOLO este campo que aparece en tu configuración:**
   - `Emulator: Emulator Path Linux` → `/home/codespace/android-sdk/emulator`

   **IMPORTANTE:** Usa la ruta del DIRECTORIO (sin "/emulator" al final)

4. **Guarda la configuración**

5. **Intenta usar el comando "Create New Android Emulator" nuevamente**

---

## 🚀 Una vez configurado:

Ejecuta nuestro script de debug para instalar y probar la APK:
```bash
/workspaces/DroneScan_V7/debug-emulator.sh
```

¡Esto nos permitirá ver los logs de error en tiempo real! 📋
