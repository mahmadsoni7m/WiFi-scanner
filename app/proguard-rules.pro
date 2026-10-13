# WiFi Scanner ProGuard / R8 rules.
# Room, Compose, DataStore and AndroidX ship their own consumer rules.
# Keep stack traces readable in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
