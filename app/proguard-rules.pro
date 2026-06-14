# ==================== GENERAL ====================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes EnclosingMethod

# ==================== FIREBASE ====================
-keep class com.google.firebase.** { *; }
-keep class com.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ==================== FIREBASE AUTH ====================
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.auth.* *;
}

# ==================== FIREBASE FIRESTORE ====================
-keep class com.google.firestore.** { *; }
-keep class com.google.common.** { *; }
-dontwarn com.google.common.**

# ==================== FIREBASE REALTIME DATABASE ====================
-keep class com.google.firebase.database.** { *; }

# ==================== FIREBASE STORAGE ====================
-keep class com.google.firebase.storage.** { *; }

# ==================== FIREBASE MESSAGING ====================
-keep class com.google.firebase.messaging.** { *; }

# ==================== AGORA SDK ====================
-keep class io.agora.** { *; }
-dontwarn io.agora.**
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# ==================== OKHTTP ====================
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# ==================== RETROFIT ====================
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# ==================== GLIDE ====================
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule

# ==================== CIRCLE IMAGE VIEW ====================
-keep class de.hdodenhof.circleimageview.** { *; }

# ==================== PHOTO VIEW ====================
-keep class com.github.chrisbanes.photoview.** { *; }

# ==================== JSON ====================
-keep class org.json.** { *; }
-dontwarn org.json.**

# ==================== GOOGLE SIGN-IN ====================
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

# ==================== GOOGLE MAPS ====================
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**

# ==================== COROUTINES ====================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ==================== MODEL CLASSES (DO NOT OBFUSCATE) ====================
-keep class com.example.goprox.ChatMessage { *; }
-keep class com.example.goprox.ChatSummary { *; }
-keep class com.example.goprox.Service { *; }
-keep class com.example.goprox.Review { *; }

# ==================== ACTIVITIES (DO NOT OBFUSCATE) ====================
-keep class com.example.goprox.SplashActivity { *; }
-keep class com.example.goprox.MainActivity { *; }
-keep class com.example.goprox.LoginActivity { *; }
-keep class com.example.goprox.RegistrationActivity { *; }
-keep class com.example.goprox.HomeActivity { *; }
-keep class com.example.goprox.ProfileActivity { *; }
-keep class com.example.goprox.ChatActivity { *; }
-keep class com.example.goprox.ChatListActivity { *; }
-keep class com.example.goprox.CallActivity { *; }
-keep class com.example.goprox.IncomingCallActivity { *; }
-keep class com.example.goprox.OutgoingCallActivity { *; }
-keep class com.example.goprox.ServiceDetailActivity { *; }
-keep class com.example.goprox.AddPostActivity { *; }
-keep class com.example.goprox.AIDialogActivity { *; }
-keep class com.example.goprox.EditProfileActivity { *; }
-keep class com.example.goprox.FullscreenImageActivity { *; }
-keep class com.example.goprox.VerifyEmailActivity { *; }
-keep class com.example.goprox.ResetPasswordActivity { *; }
-keep class com.example.goprox.ComplaintActivity { *; }
-keep class com.example.goprox.NotFoundActivity { *; }

# ==================== ADAPTERS & MANAGERS ====================
-keep class com.example.goprox.ChatAdapter { *; }
-keep class com.example.goprox.ChatListAdapter { *; }
-keep class com.example.goprox.ServiceAdapter { *; }
-keep class com.example.goprox.SpecialistAdapter { *; }
-keep class com.example.goprox.ProfilePostsAdapter { *; }
-keep class com.example.goprox.ReviewAdapter { *; }
-keep class com.example.goprox.CallManager { *; }
-keep class com.example.goprox.CallHelper { *; }
-keep class com.example.goprox.AudioRecorder { *; }
-keep class com.example.goprox.FirebaseService { *; }
-keep class com.example.goprox.BaseActivity { *; }

# ==================== BUILD CONFIG ====================
-keep class com.example.goprox.BuildConfig { *; }

# ==================== REMOVE LOGGING IN RELEASE ====================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}