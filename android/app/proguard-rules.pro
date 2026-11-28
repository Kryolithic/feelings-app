# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK proguard-android-optimize.txt.

# Required by kotlinx.serialization and generic type inspection under R8
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

-keep @kotlinx.serialization.Serializable class * {
    *;
}

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

-keep,includedescriptorclasses class com.example.feelingsapp.data.**$$serializer {
    *;
}

-keepclassmembers class com.example.feelingsapp.data.LogEntry$Companion {
    kotlinx.serialization.KSerializer serializer();
}

-keepclassmembers class com.example.feelingsapp.data.User$Companion {
    kotlinx.serialization.KSerializer serializer();
}

# WorkManager reflectively instantiates ListenableWorker subclasses
-keep class com.example.feelingsapp.notifications.MoodReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
