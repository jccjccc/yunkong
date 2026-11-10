# OkHttp / kotlinx.serialization
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations

# kotlinx.serialization generated serializers
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.dashabi.control.**$$serializer { *; }
-keepclassmembers class com.dashabi.control.** {
    *** Companion;
}
-keepclasseswithmembers class com.dashabi.control.** {
    kotlinx.serialization.KSerializer serializer(...);
}
