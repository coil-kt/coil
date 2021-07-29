# This is a configuration file for ProGuard.
# https://proguard.sourceforge.net/index.html#manual/usage.html

-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Preserve some attributes that may be required for reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# For native methods, see https://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * { native <methods>; }

# Keep setters in views so that animations can still work.
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# For enumeration classes, see https://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }

# Preserve annotated Javascript interface methods.
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }

# The support libraries contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version. We know about them, and they are safe.
-dontnote androidx.**
-dontwarn androidx.**

# Understand the @Keep support annotation.
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep <methods>; }
-keepclasseswithmembers class * { @androidx.annotation.Keep <fields>; }
-keepclasseswithmembers class * { @androidx.annotation.Keep <init>(...); }

# These classes are duplicated between android.jar and org.apache.http.legacy.jar.
-dontnote org.apache.http.**
-dontnote android.net.http.**

# These classes are duplicated between android.jar and core-lambda-stubs.jar.
-dontnote java.lang.invoke.**
