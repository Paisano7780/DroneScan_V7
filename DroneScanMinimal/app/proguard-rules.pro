# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# DJI SDK ProGuard Rules
-keep class dji.** { *; }
-keep class com.dji.** { *; }
-keepclassmembers class dji.** { *; }
-keepclassmembers class com.dji.** { *; }

# DJI SDK Manager específico
-keep class dji.sdk.sdkmanager.DJISDKManager { *; }
-keep class dji.sdk.sdkmanager.** { *; }
-keepclassmembers class dji.sdk.sdkmanager.** { *; }

# DJI Error classes
-keep class dji.common.error.** { *; }
-keep class dji.sdk.sdkmanager.DJISDKError { *; }

# Mantener constructores
-keepclassmembers class dji.** {
    <init>(...);
}

# Evitar obfuscación de callbacks
-keep interface dji.** { *; }

# DJI Reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# USB Host related
-keep class android.hardware.usb.** { *; }
-keep class com.dronescan.msdksample.usb.** { *; }

# Debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
