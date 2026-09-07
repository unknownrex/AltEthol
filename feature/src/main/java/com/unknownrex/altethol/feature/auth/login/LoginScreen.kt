package com.unknownrex.altethol.feature.auth.login

import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unknownrex.altethol.core.ui.ObserveAsEvents
import com.unknownrex.altethol.core.ui.text.UiText
import com.unknownrex.altethol.core.ui.text.asString
import com.unknownrex.altethol.feature.R
import com.unknownrex.altethol.feature.auth.CookieParser
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val LOGIN_URL = "https://ethol.pens.ac.id/api/auth/cas-redirect"

private const val ETHOL_COOKIE_ORIGIN = "https://ethol.pens.ac.id"

private const val COOKIE_POLL_INTERVAL_MS = 400L

private const val TAG = "AltEtholLoginWeb"

private fun logCurrentUrl(url: String?) {
    Log.d(TAG, "onPage url=$url")
}

private fun etholCookieString(): String =
    CookieManager.getInstance().getCookie(ETHOL_COOKIE_ORIGIN).orEmpty()

private fun hasTokenCookie(): Boolean = CookieParser.extractToken(etholCookieString()) != null

@Composable
fun LoginRoot(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.NavigateToHome -> onLoginSuccess()
        }
    }

    LoginScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AuthWebView(
            onLoginSuccess = { onAction(LoginAction.OnLoginSuccess(it)) },
            reloadTrigger = state.reloadTrigger,
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isSaving) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        state.error?.let { message ->
            LoginErrorBanner(
                message = message,
                onReload = { onAction(LoginAction.OnReload) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun AuthWebView(
    onLoginSuccess: (String) -> Unit,
    reloadTrigger: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val successHandled = remember { mutableStateOf(false) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    val fireSuccess: () -> Unit = {
        successHandled.value = true
        val cookies = etholCookieString()
        Log.d(TAG, "Auth cookies detected, cookies=$cookies")
        webViewRef.value?.stopLoading()
        onLoginSuccess(cookies)
    }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    logCurrentUrl(url)
                    if (!successHandled.value && hasTokenCookie()) fireSuccess()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    logCurrentUrl(url)
                    if (!successHandled.value && hasTokenCookie()) fireSuccess()
                }
            }
        }.also { webViewRef.value = it }
    }

    val loadLoginPage: () -> Unit = {
        successHandled.value = false
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        webView.loadUrl(LOGIN_URL)
    }

    LaunchedEffect(webView, reloadTrigger) {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        loadLoginPage()
        while (!successHandled.value) {
            delay(COOKIE_POLL_INTERVAL_MS)
            if (hasTokenCookie()) fireSuccess()
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
}

@Composable
private fun LoginErrorBanner(
    message: UiText,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message.asString(),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onReload) {
                Text(stringResource(R.string.login_reload))
            }
        }
    }
}
