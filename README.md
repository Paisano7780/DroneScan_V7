
# DroneScan_V7 — Descripción Técnica y Guía de Reconstrucción

## Descripción General
DroneScan_V7 es una aplicación Android para la gestión y escaneo de fotos tomadas por drones DJI, modernizada y optimizada para funcionar con el SDK DJI Mobile V5.15.0. El proyecto ha sido migrado completamente a Kotlin, eliminando duplicados y dependencias Java innecesarias, y asegurando compatibilidad con dispositivos modernos y sistemas como HyperOS.

## Principales acciones realizadas
- **Migración total a Kotlin**: Todo el código relevante fue convertido a Kotlin, eliminando clases y fragments Java redundantes.
- **Integración y limpieza de Gradle**: Se revisaron y ajustaron los archivos `build.gradle` y `settings.gradle` para asegurar dependencias mínimas y build limpio.
- **Compatibilidad DJI SDK V5.15.0**: Se adaptó la lógica de obtención y descarga de la última foto usando `MediaManager` y filtrado robusto por nombre de archivo, descartando APIs no públicas o inestables.
- **Eliminación de código basura**: Se identificaron y eliminaron módulos, utilidades y recursos no usados, y se documentó una lista de limpieza adicional en `LIMPIEZA_SUGERIDA.md`.
- **Centralización de logs y errores**: Se implementó un logger global (`ErrorLogger`) que registra cualquier excepción o crash en un archivo de texto en la carpeta Documentos del dispositivo, facilitando el debug en campo.
- **Permisos y compatibilidad HyperOS/Xiaomi**: Se ajustó la gestión de permisos para asegurar acceso a almacenamiento y funcionamiento en dispositivos con restricciones modernas.

## Lógica principal
1. **Escaneo y descarga de fotos**: Al conectar el dron, la app escanea la SD, filtra las fotos JPEG, identifica la más reciente por nombre correlativo y la descarga localmente.
2. **Procesamiento y escaneo de código de barras**: Tras la descarga, la imagen se pasa a un módulo de escaneo de códigos de barras y, si es exitoso, se genera un CSV con los datos.
3. **Gestión de errores**: Cualquier error crítico se muestra en pantalla y se registra en el log global accesible desde la app de archivos del dispositivo.

## Recomendaciones para reconstrucción desde cero
1. **Estructura mínima**: Mantener solo los módulos y clases estrictamente necesarios para la lógica de escaneo, descarga y procesamiento de fotos.
2. **Integrar DJI SDK V5.15.0**: Seguir la lógica de MediaManager y evitar APIs no documentadas.
3. **Centralizar logs y manejo de errores**: Implementar un logger global desde el inicio.
4. **Permisos y compatibilidad**: Priorizar permisos de almacenamiento y pruebas en dispositivos Xiaomi/HyperOS.
5. **Documentar y limpiar**: Mantener una lista de limpieza y optimización como `LIMPIEZA_SUGERIDA.md`.

## Recursos clave
- `DroneScanActivity.kt`: Lógica principal de escaneo y descarga.
- `ErrorLogger.kt`: Logger global de errores.
- `LIMPIEZA_SUGERIDA.md`: Sugerencias de limpieza y optimización.

---
Este README sirve como referencia técnica para reconstruir el proyecto en caso de desastre, asegurando que los aprendizajes y buenas prácticas implementadas no se pierdan.
