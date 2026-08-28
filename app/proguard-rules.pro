# Gson
-keep class com.opoleyes.data.model.** { *; }
-keep class com.opoleyes.data.local.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
# TypeToken y sus subclases anónimas (object : TypeToken<T>() {}) usan reflexión
# sobre la información genérica; R8 la elimina si no se conserva.
-keep,allowobfuscation class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
# Gson usa reflexión sobre campos y constructores sin args
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    <init>();
}

# Lottie / dotlottie-android
-keep class com.airbnb.lottie.** { *; }
-keep class com.dotlottie.dlplayer.** { *; }
-dontwarn com.airbnb.lottie.**
-dontwarn com.dotlottie.dlplayer.**

# Compose
-dontwarn androidx.compose.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
