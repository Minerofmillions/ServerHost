-keep enum ** { *; }

-keepattributes *Annotation*,EnclosingMethod,Signature,SourceFile,InnerClasses

-dontwarn com.fasterxml.jackson.databind.**

-keep public class com.arkivanov.decompose.extensions.compose.mainthread.SwingMainThreadChecker { *; }