-keep class com.deepseek.harness.webview.AndroidJsBridge { *; }
-keepclassmembers class com.deepseek.harness.webview.AndroidJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
