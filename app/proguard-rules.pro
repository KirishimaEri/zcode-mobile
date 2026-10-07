# Compose / Material3 由自带 consumer 规则处理，以下仅补充反射敏感点

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# org.json 走 Android 框架实现，但字段名被 DataStore 序列化逻辑以字符串引用，保留类名即可
-dontwarn org.json.**

# ML Kit / Play Services 自带 consumer 规则，避免剔除 native 桥接
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# WebView 与官方页面的 JS 交互仅通过 DOM 文本检查，无需 keep
