# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-printmapping mapping.txt

-dontwarn android.util.StatsEvent$Builder
-dontwarn android.util.StatsEvent
-dontwarn org.commonmark.ext.gfm.strikethrough.Strikethrough
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.PropertyDescriptor
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn sun.misc.BASE64Decoder
-dontwarn sun.misc.BASE64Encoder
-dontwarn sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
-keep class org.pytorch.**{*;}
-keep class me.ag2s.**{*;}
-keep class org.script.**{*;}
-keep class com.github.**{*;}
-keep class com.facebook.**{*;}
-keep class java.**{*;}
-keep class sun.**{*;}
-keep class ai.mlc.mlcllm.**{*;}
-keep class org.apache.tvm.**{*;}
-dontwarn com.jeremyliao.liveeventbus.**
-keep class com.jeremyliao.liveeventbus.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.arch.core.** { *; }
-keep class ltd.finelink.read.data.entities.**{*;}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes Signature,AnnotationDefault,RuntimeVisibleAnnotations
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
