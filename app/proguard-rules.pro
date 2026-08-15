-keep class cn.zjx521.deepseek.harness.webview.AndroidJsBridge { *; }
-keepclassmembers class cn.zjx521.deepseek.harness.webview.AndroidJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
