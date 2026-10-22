# The game has no reflection, but keeping the small class set makes crash
# reports readable and avoids any R8 surprises with Android framework callbacks.
-keep class com.deepfalldrill.game.** { *; }

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
