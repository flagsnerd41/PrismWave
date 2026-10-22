# NewPipeExtractor (reflection, Rhino JS engine, protobuf, nanojson, jsoup)
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-keep class com.grack.nanojson.** { *; }
-keep class org.jsoup.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn **

# keep line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable
