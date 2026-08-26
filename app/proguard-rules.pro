# Gson
-keep class com.opoleyes.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# AdMob
-keep class com.google.android.gms.ads.** { *; }

# Lottie / dotlottie-android
-keep class com.airbnb.lottie.** { *; }
-keep class com.dotlottie.dlplayer.** { *; }
-dontwarn com.airbnb.lottie.**
-dontwarn com.dotlottie.dlplayer.**

# Compose
-dontwarn androidx.compose.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
