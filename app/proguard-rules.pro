# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Model request/response Retrofit+Gson (site.elahady.alkaukaba.model & .api) -
# sebagian besar field-nya TIDAK pakai @SerializedName, jadi Gson bergantung pada
# nama field Kotlin persis sama dengan key JSON (mis. Timings, AuthModels,
# GoogleLoginRequest). Tanpa -keep ini, R8 mengacak nama field itu dan
# deserialisasi gagal diam-diam (login/waktu sholat/kiblat rusak tanpa crash).
-keep class site.elahady.alkaukaba.model.** { *; }
-keep class site.elahady.alkaukaba.api.** { *; }
-keepattributes Signature,*Annotation*