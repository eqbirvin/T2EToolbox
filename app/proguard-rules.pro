# Add project specific ProGuard rules here.

# Shizuku keep rules
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-keepclassmembers class rikka.shizuku.Shizuku {
    private static rikka.shizuku.ShizukuRemoteProcess newProcess(java.lang.String[], java.lang.String[], java.lang.String);
}
-keep class rikka.shizuku.ShizukuRemoteProcess { *; }

# WorkManager Worker
-keep class androidx.work.** { *; }
-keep class com.t2e.toolbox.eqbirvin.worker.** { *; }

-keepattributes *Annotation*,InnerClasses
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** {
    *** Companion;
}
