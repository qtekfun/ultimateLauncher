# Reglas de R8 de UltimateLauncher (variante release). Ver docs/arranque-en-frio.md.
# Base: las reglas de Launcher3 (se mantienen los nombres de com.android.**; solo se elimina código no usado).
-include ../launcher3-base/proguard.flags

# Código propio: nombres estables para pila de errores legible y para los intents/manifiestos.
-keep class com.qtekfun.** { *; }

# Inflado de vistas por nombre desde XML (Launcher3 y propias) y atributos reflejados por Preference/Fragment.
-keep class * extends android.view.View { <init>(...); }
-keep class * extends androidx.preference.Preference { <init>(...); }

# Protobuf lite: los campos se resuelven por reflexión a partir del descriptor.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Animadores por reflexión (setX/getX por nombre de propiedad).
-keepclassmembers class * { public void set*(float); public float get*(); }

# Clases que provee la plataforma en tiempo de ejecución (extensiones de ventana, sidecar y AppFunctions del sistema);
# no están en el classpath de compilación (lista de missing_rules.txt de R8, revisada a mano).
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
-dontwarn com.android.extensions.appfunctions.**
