# ARJUN VISION TV release rules.
# WorkManager instantiates ThemeWorker by class name.
-keep class com.arjunvision.tvlauncher.platform.work.ThemeWorker { <init>(...); }
# Keep line numbers for readable crash logs.
-keepattributes SourceFile,LineNumberTable
