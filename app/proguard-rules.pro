# Add project specific ProGuard rules here.
-keep class dev.rikka.shizuku.** { *; }
-dontwarn dev.rikka.shizuku.**

-keepattributes *Annotation*,InnerClasses
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** {
    *** Companion;
}
