# Add project specific ProGuard rules here.

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, AnnotationDefault
-keepclassmembers class com.squareup.retrofit2.** { *; }
-dontwarn com.squareup.retrofit2.**
-keep class com.squareup.retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# General
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keep class com.example.model.** { *; }
-keep class com.example.ai.** { *; }
