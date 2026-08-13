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
import org.koin.androidx.compose.koinViewModel

private const val LOGIN_URL =
    "https://login.pens.ac.id/cas/login?service=http%3A%2F%2Fethol.pens.ac.id%2Fcas%2F"

private const val DASHBOARD_URL_SEGMENT = "/mahasiswa/beranda"

private const val TAG = "AltEtholLoginWeb"

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
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            var successHandled = false
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    handleDashboardReached(url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    handleDashboardReached(url)
                }

                private fun handleDashboardReached(url: String?) {
                    val current = url ?: return
                    Log.d(TAG, "onPage url=$current")
                    if (!successHandled && current.contains(DASHBOARD_URL_SEGMENT)) {
                        successHandled = true
                        val cookies = CookieManager.getInstance().getCookie(current).orEmpty()
                        Log.d(TAG, "Dashboard reached, cookies=$cookies")
                        onLoginSuccess(cookies)
                    }
                }
            }
        }
    }

    LaunchedEffect(webView) {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.loadUrl(LOGIN_URL)
    }

    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) webView.reload()
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
