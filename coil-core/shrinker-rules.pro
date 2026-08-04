-dontwarn coil3.PlatformContext

# Prevent R8 9.0+ from vertically merging GenericViewTarget into any of
# its subclasses. That merge silently drops `super` calls to Target's
# default onStart/onSuccess/onError methods, which can cause images to
# fail to render in release builds.
# https://issuetracker.google.com/issues/524864608
-keep class * extends coil3.target.GenericViewTarget { *; }
