# Sugerencia de limpieza y optimización del proyecto DroneScan_V7

## 1. Paquetes y archivos potencialmente eliminables
- `Mobile-SDK-Android-V5/SampleCode-V5/android-sdk-v5-sample/src/main/java/dji/sampleV5/aircraft/util/wheel/`
  - `WheelRecycle.java` y clases asociadas (si no usas controles tipo rueda)
- `Mobile-SDK-Android-V5/SampleCode-V5/android-sdk-v5-uxsdk/`
  - Todo el módulo si no usas la UX SDK ni mapas avanzados
  - Clases de mapas: `mapkit/maplibre`, `core/maps`, etc.
- `Mobile-SDK-Android-V5/SampleCode-V5/android-sdk-v5-sample/src/main/java/dji/sampleV5/aircraft/util/`
  - `DrawUtils.java`, `Loader.java`, `UxSharedPreferencesUtil.java` (si no hay referencias)
- `Mobile-SDK-Android-V5/Docs/Android_API/`
  - Documentación y scripts JS (no afectan el APK, pero ocupan espacio)
- Recursos no usados: imágenes, layouts, strings huérfanos
- Archivos de configuración duplicados: `local.properties`, `errorlog.txt` en varios módulos

## 2. Acciones recomendadas
- Ejecutar "Optimize Imports" en todos los archivos fuente
- Revisar y limpiar dependencias en `build.gradle`
- Buscar y eliminar clases, fragments y activities no referenciados
- Usar Lint o "Inspect Code" para encontrar código muerto y recursos huérfanos
- Eliminar módulos completos no usados desde `settings.gradle`

## 3. Notas
- Antes de eliminar, verifica que no haya referencias cruzadas o dependencias ocultas.
- Haz un backup o commit antes de limpiar para poder revertir si es necesario.

---

Esta lista es una guía inicial. Se recomienda revisar cada archivo antes de eliminarlo definitivamente.
