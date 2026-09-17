# Room generates code reflectively-referenced by annotation processing; keep entities/DAOs intact.
-keep class com.salimshivani.inventoryscanner.data.** { *; }

# AndroidX/Material reference their own R$* inner classes for optional attrs/styles that
# don't exist in every consuming app; R8 (with android.nonTransitiveRClass) can't always
# resolve them, but that's fine — they're only touched by unused code paths. Suppresses the
# "missing classes" build failure AGP itself generates these exact rules for.
-dontwarn androidx.core.R$attr
-dontwarn androidx.core.R$id
-dontwarn androidx.core.R$styleable
-dontwarn androidx.recyclerview.R$styleable
-dontwarn com.google.android.material.R$attr
-dontwarn com.google.android.material.R$dimen
-dontwarn com.google.android.material.R$string
-dontwarn com.google.android.material.R$style
-dontwarn com.google.android.material.R$styleable
