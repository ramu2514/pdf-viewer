 #skip every public class that extends com.orm.SugarRecord and their public/protected members
-keep public class * extends com.orm.SugarRecord {*;}
-keep class com.orm.** { *; }

#Keep all fragments
-keep public class * extends androidx.fragment.app.Fragment

-keepclasseswithmembers class * {
    native <methods>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers class * {
    native <methods>;
}

-keep class com.artifex.mupdf.fitz.** {*;}

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod
