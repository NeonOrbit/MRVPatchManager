-dontoptimize
-dontobfuscate
-keepattributes Signature, *Annotation*
-keepattributes SourceFile, LineNumberTable

-keep class app.neonorbit.mrvpatchmanager.** {*;}
-keep class org.simpleframework.xml.** {*;}
-keep class com.google.gson.annotations.** {*;}
-keep class pl.droidsonroids.jspoon.annotation.** {*;}

-keep class org.lsposed.** {*;}
-keep class com.android.apksig.** {*;}
-keep class com.beust.jcommander.** {*;}
-keep class com.android.tools.build.apkzlib.** {*;}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn android.content.res.**
-dontwarn org.xmlpull.v1.**
-dontwarn org.openjsse.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn com.google.auto.value.**