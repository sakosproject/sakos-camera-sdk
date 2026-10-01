# AndroidJUnitRunner needs this shared AndroidX dependency when instrumenting a
# minified target. Only the test-key localRuntime variant retains it; release
# and all SDK/LiteRT shrinking remain subject to the normal consumer rules.
-keep class androidx.tracing.Trace { *; }
# AndroidX test storage uses Kotlin classes which the target application may
# otherwise inline/remove. Preserve test runner support in this variant only.
-keep class kotlin.** { *; }
