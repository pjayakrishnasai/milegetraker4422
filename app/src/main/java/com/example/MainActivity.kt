package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

import java.io.File

class MainActivity : ComponentActivity() {

  private var webView: WebView? = null
  private var filePathCallback: ValueCallback<Array<Uri>>? = null

  private val fileChooserLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == RESULT_OK) {
      val data: Intent? = result.data
      val results: Array<Uri>? = when {
        data?.dataString != null -> arrayOf(Uri.parse(data.dataString))
        data?.clipData != null -> {
          val count = data.clipData?.itemCount ?: 0
          Array(count) { i -> data.clipData!!.getItemAt(i).uri }
        }
        else -> null
      }
      filePathCallback?.onReceiveValue(results)
    } else {
      filePathCallback?.onReceiveValue(null)
    }
    filePathCallback = null
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    try {
      val baseCache = File(cacheDir, "WebView/Default/HTTP Cache")
      File(baseCache, "Code Cache/js").mkdirs()
      File(baseCache, "Code Cache/wasm").mkdirs()
    } catch (_: Exception) {}

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (webView?.canGoBack() == true) {
          webView?.goBack()
        } else {
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
        }
      }
    })

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
          color = Color(0xFF0F172A)
        ) {
          TrackerWebView(
            onWebViewCreated = { webView = it },
            onOpenFileChooser = { callback, intent ->
              filePathCallback?.onReceiveValue(null)
              filePathCallback = callback
              try {
                fileChooserLauncher.launch(intent)
              } catch (e: Exception) {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
              }
            }
          )
        }
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrackerWebView(
  onWebViewCreated: (WebView) -> Unit,
  onOpenFileChooser: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context ->
      WebView(context).apply {
        setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          allowFileAccess = true
          allowContentAccess = true
          loadWithOverviewMode = true
          useWideViewPort = true
          cacheMode = WebSettings.LOAD_DEFAULT
        }
        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            if (url.startsWith("file://") || url.startsWith("data:") || url.startsWith("blob:")) {
              return false
            }
            return try {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
              context.startActivity(intent)
              true
            } catch (e: Exception) {
              false
            }
          }
        }
        webChromeClient = object : WebChromeClient() {
          override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
          ): Boolean {
            if (filePathCallback != null) {
              val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
              }
              onOpenFileChooser(filePathCallback, intent)
              return true
            }
            return false
          }
        }
        loadUrl("file:///android_asset/index.html")
        onWebViewCreated(this)
      }
    }
  )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  androidx.compose.material3.Text(
    text = "Hello $name!",
    modifier = modifier
  )
}
