# kotlinx.serialization: keep generated serializers for our @Serializable models
# (DTOs, persisted favorites/history and navigation routes).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class io.github.poodicraft.serverscope.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.poodicraft.serverscope.**$$serializer { *; }
-keepclassmembers class io.github.poodicraft.serverscope.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit service interface (Retrofit 3 ships its own consumer rules for the library itself).
-keep interface io.github.poodicraft.serverscope.data.remote.McStatusApi { *; }
