# Versión de Java Requerida para DroneScan_V7

## Respuesta Rápida
**Esta aplicación requiere Java 17 para compilar correctamente.**

## Análisis Detallado

### Configuración Actual del Proyecto

#### Android Gradle Plugin y Compatibilidad
- **Android Gradle Plugin**: 8.1.0
- **Gradle Version**: 8.2  
- **Kotlin Version**: 1.7.22

#### Configuración de Compatibilidad Java
```gradle
// En app/build.gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8  // Java 8
    targetCompatibility JavaVersion.VERSION_1_8  // Java 8
}

kotlinOptions {
    jvmTarget = '1.8'  // Kotlin compila a bytecode Java 8
}
```

### Requisitos de Versión Java

#### Para Compilación (Build System)
- **Mínimo**: Java 17
- **Recomendado**: Java 17 LTS
- **Razón**: Android Gradle Plugin 8.1.0 requiere Java 17 como mínimo

#### Para Ejecución en Android
- **Target**: Java 8 (Android compatible)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

### Configuración del Entorno

#### 1. Instalar Java 17
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# macOS (usando Homebrew)
brew install openjdk@17

# Windows
# Descargar desde https://adoptium.net/ o usar Chocolatey:
choco install openjdk17
```

#### 2. Configurar Variables de Entorno
```bash
# Linux/macOS
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Windows
# Configurar JAVA_HOME en Variables del Sistema
# JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot
```

#### 3. Verificar Instalación
```bash
java -version
# Debería mostrar: openjdk version "17.x.x"

javac -version
# Debería mostrar: javac 17.x.x
```

### Comandos de Compilación

#### Script Automatizado (Recomendado)
```bash
# Usar el script de debug incluido
./debug-celular.sh
```

#### Compilación Manual
```bash
cd DroneScanMinimal

# Configurar JAVA_HOME si es necesario
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Compilar APK de debug
./gradlew assembleDebug

# Compilar APK de release
./gradlew assembleRelease
```

### Compatibilidad de Versiones

| Componente | Versión Mínima | Versión Usada | Requiere Java |
|------------|---------------|---------------|---------------|
| Android Gradle Plugin | 8.0+ | 8.1.0 | Java 17+ |
| Gradle | 8.0+ | 8.2 | Java 17+ |
| Kotlin | 1.7+ | 1.7.22 | Java 17+ |
| DJI SDK | 4.16+ | 4.16.4 | Java 8+ |

### Solución de Problemas

#### Error: "Java home supplied is invalid"
```bash
# Verificar la ruta de Java
which java
echo $JAVA_HOME

# Actualizar gradle.properties si es necesario
# Comentar la línea org.gradle.java.home si no es correcta
```

#### Error: "Unsupported Java version"
```bash
# Verificar que tienes Java 17 o superior
java -version

# Si tienes múltiples versiones, asegurar que JAVA_HOME apunta a Java 17+
export JAVA_HOME=/path/to/java-17
```

#### Error de compilación de DJI SDK
```bash
# Asegurar que las dependencias DJI están correctamente configuradas
# El proyecto ya incluye la configuración necesaria en build.gradle
```

### Notas Importantes

1. **Java 17 es obligatorio** para el proceso de compilación debido a Android Gradle Plugin 8.1.0
2. **El código de la app** está compilado para ser compatible con Java 8 (Android)
3. **DJI SDK** funciona correctamente con esta configuración
4. **La APK resultante** es compatible con Android 8.0+ (API 26+)

### Versiones Testadas

- ✅ **Java 17.0.16** (OpenJDK Temurin) - Totalmente compatible
- ✅ **Java 17.0.x** (cualquier versión 17 LTS) - Recomendado
- ❌ **Java 11** - No compatible con Android Gradle Plugin 8.1.0
- ❌ **Java 8** - No compatible con Android Gradle Plugin 8.1.0

---

**Actualizado**: Enero 2025  
**Proyecto**: DroneScan_V7 v2.14  
**Estado**: Validado y funcionando