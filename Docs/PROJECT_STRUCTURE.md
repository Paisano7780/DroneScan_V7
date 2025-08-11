# Estructura Principal del Proyecto DroneScan_V7

## Carpetas y Módulos Relevantes

- **Mobile-SDK-Android-V5/**
  - Docs/ (Documentación del SDK DJI)
  - SampleCode-V5/
    - android-sdk-v5-as/   ← Módulo base para compilación (build global)
    - android-sdk-v5-uxsdk/   ← Módulo de utilidades y widgets de interfaz
    - android-sdk-v5-sample/  ← Módulo de ejemplo principal

## Módulo Base para Build
- El build global y los comandos Gradle deben ejecutarse desde:
  `Mobile-SDK-Android-V5/SampleCode-V5/android-sdk-v5-as`
  Ejemplo: `cd Mobile-SDK-Android-V5/SampleCode-V5/android-sdk-v5-as && ./gradlew clean assembleDebug`

## Paquetes Principales
- android-sdk-v5-as: (varía según configuración)
- android-sdk-v5-uxsdk: `dji.v5.ux`
- android-sdk-v5-sample: `dji.sampleV5.aircraft`

## Ruta Recomendada para Utilidades/Stubs
- Guardar utilidades mínimas en:
  `Mobile-SDK-Android-V5/SampleCode-V5/android-sdk-v5-uxsdk/src/main/java/dji/v5/ux/utils/`

## Notas de Sesión
- Si se retoma el trabajo, consultar este archivo para saber:
  - Dónde ejecutar el build
  - Dónde agregar utilidades
  - Qué módulos son relevantes
  - Paquetes base

---
Actualizado: 11/08/2025
