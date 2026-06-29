# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.tame.app.data.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.tame.app.data.model.**$$serializer { *; }
-keepclassmembers class com.tame.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
