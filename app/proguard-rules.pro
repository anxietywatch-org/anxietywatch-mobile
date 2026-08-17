# Reglas minimas para que R8 no rompa Retrofit/kotlinx.serialization/Room al minificar.
# Sin esto, el APK de release compila pero truena en tiempo de ejecucion con
# "No serializer found" o crashes de reflexion -- error clasico de minificar sin reglas.

-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# kotlinx.serialization: conserva los serializers generados y las clases @Serializable
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.anxietywatch.mobile.data.remote.**$$serializer { *; }
-keepclassmembers class com.anxietywatch.mobile.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.anxietywatch.mobile.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Exceptions

# Room genera codigo en tiempo de compilacion; no necesita reglas extra normalmente,
# pero se deja explicito por si el equipo agrega TypeConverters mas adelante.
-keep class com.anxietywatch.mobile.data.local.** { *; }
