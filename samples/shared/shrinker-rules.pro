# This is a configuration file for ProGuard.
# https://proguard.sourceforge.net/index.html#manual/usage.html

-allowaccessmodification
-flattenpackagehierarchy
-mergeinterfacesaggressively
-dontnote *
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Remove intrinsic assertions.
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkFieldIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
}
