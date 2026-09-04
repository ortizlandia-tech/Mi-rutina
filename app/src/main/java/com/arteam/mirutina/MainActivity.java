package com.arteam.mirutina;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.media.AudioManager;
import android.media.ToneGenerator;

import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private TextToSpeech tts;
    private ToneGenerator tone;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(android.graphics.Color.rgb(12,66,87));
        w.setNavigationBarColor(android.graphics.Color.rgb(12,66,87));

        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale ar = new Locale("es", "AR");
                tts.setLanguage(ar);
                tts.setSpeechRate(0.96f);
                tts.setPitch(1.05f);
                try {
                    Voice best = null;
                    for (Voice v : tts.getVoices()) {
                        Locale l = v.getLocale();
                        if (l != null && "es".equalsIgnoreCase(l.getLanguage()) && "AR".equalsIgnoreCase(l.getCountry())) {
                            if (best == null) best = v;
                            String n = v.getName().toLowerCase(Locale.ROOT);
                            if (n.contains("female") || n.contains("femen") || n.contains("woman")) { best = v; break; }
                        }
                    }
                    if (best != null) tts.setVoice(best);
                } catch (Exception ignored) {}
            }
        });

        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        webView.addJavascriptInterface(new WorkoutBridge(), "WorkoutNative");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class WorkoutBridge {
        @JavascriptInterface public void speak(String text) {
            runOnUiThread(() -> {
                if (tts != null && text != null && !text.trim().isEmpty()) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "workout");
                }
            });
        }

        @JavascriptInterface public void stopSpeech() {
            runOnUiThread(() -> { if (tts != null) tts.stop(); });
        }

        @JavascriptInterface public void beep(String kind) {
            runOnUiThread(() -> {
                if (tone == null) return;
                if ("go".equals(kind)) tone.startTone(ToneGenerator.TONE_PROP_ACK, 280);
                else if ("finish".equals(kind)) tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 420);
                else tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
            });
        }
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (tone != null) tone.release();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript("window.handleAndroidBack ? window.handleAndroidBack() : false", value -> {
                if ("true".equals(value)) return;
                if (webView.canGoBack()) webView.goBack();
                else MainActivity.super.onBackPressed();
            });
        } else super.onBackPressed();
    }
}
