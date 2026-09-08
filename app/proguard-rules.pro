# Standard Android keeps
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Room database and implementations (created via reflection Room.databaseBuilder)
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
    *;
}
-keep class **_Impl {
    <init>(...);
    *;
}

# Keep Room entities and DAOs
-keep class com.example.connectany.data.local.entity.** { *; }
-keep class com.example.connectany.data.local.dao.** { *; }
-keep class com.example.connectany.data.local.** { *; }

# Keep domain models and settings
-keep class com.example.connectany.domain.model.** { *; }
-keep class com.example.connectany.data.settings.** { *; }
-keep class com.example.connectany.data.bluetooth.** { *; }

# Keep Navigation and NavController
-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }
-keep class androidx.navigation.NavType { *; }

# Keep ViewModels and Hilt Injection
-keep class com.example.connectany.presentation.**ViewModel { *; }
-keep class com.example.connectany.ui.**ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}

# Keep Enums safely
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public java.lang.String name();
}

# Keep Glance App Widget
-keep class com.example.connectany.runtime.widget.** { *; }

# Keep Services and Receivers
-keep class com.example.connectany.runtime.overlay.** { *; }
-keep class com.example.connectany.runtime.bluetooth.** { *; }

# Coroutines and Serialization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    public <init>();
}

