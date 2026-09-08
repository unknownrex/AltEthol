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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val LOGIN_URL = "https://ethol.pens.ac.id/api/auth/cas-redirect"

private const val ETHOL_COOKIE_ORIGIN = "https://ethol.pens.ac.id"

private const val ETHOL_COOKIE_PATH = "https://ethol.pens.ac.id/api/auth/cas-callback"

private const val BERANDA_PATH = "/mahasiswa/beranda"

private const val COOKIE_POLL_INTERVAL_MS = 400L

private const val REFRESH_TOKEN_TIMEOUT_MS = 10_000L

private const val MAX_RELOAD_ATTEMPTS = 3

private const val TAG = "AltEtholLoginWeb"

private fun logCurrentUrl(url: String?) {
    Log.d(TAG, "onPage url=$url")
}

private fun etholCookieString(): String =
    CookieManager.getInstance().getCookie(ETHOL_COOKIE_PATH).orEmpty()

private fun logCookiePathComparison() {
    fun summarize(cookieStr: String) = buildString {
        append("token=")
        append(CookieParser.extractToken(cookieStr) != null)
        append(" refreshToken=")
        append(CookieParser.extractRefreshToken(cookieStr) != null)
    }
    val originRead = CookieManager.getInstance().getCookie(ETHOL_COOKIE_ORIGIN).orEmpty()
    val pathRead = CookieManager.getInstance().getCookie(ETHOL_COOKIE_PATH).orEmpty()
    Log.d(
        TAG,
        "Cookie read @origin: ${summarize(originRead)} | @/api/auth/cas-callback: ${summarize(pathRead)}",
    )
}

private fun hasTokenCookie(): Boolean = CookieParser.extractToken(etholCookieString()) != null

private fun hasRefreshTokenCookie(): Boolean = CookieParser.extractRefreshToken(etholCookieString()) != null

private fun hasRequiredCookies(): Boolean = hasTokenCookie() && hasRefreshTokenCookie()

@Composable
fun LoginRoot(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.NavigateToHome -> onLoginSuccess()
            is LoginEvent.ShowStatus -> scope.launch {
                snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LoginScreen(
            state = state,
            onAction = viewModel::onAction,
            onStatus = { viewModel.onAction(LoginAction.OnStatusChange(it)) },
            modifier = modifier.padding(innerPadding),
        )
    }
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
    onStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AuthWebView(
            onLoginSuccess = { onAction(LoginAction.OnLoginSuccess(it)) },
            onStatus = onStatus,
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
    onStatus: (String) -> Unit,
    reloadTrigger: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val successHandled = remember { mutableStateOf(false) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val tokenDetectedAt = remember { mutableLongStateOf(0L) }
    val refreshAttempts = remember { mutableIntStateOf(0) }
    val lastFinishedUrl = remember { mutableStateOf<String?>(null) }

    val fireSuccess: (String) -> Unit = { message ->
        successHandled.value = true
        val cookies = etholCookieString()
        Log.d(TAG, "Auth cookies detected, hasRefreshToken=${hasRefreshTokenCookie()}, cookies=$cookies")
        logCookiePathComparison()
        onStatus(message)
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
                    if (refreshAttempts.intValue > 1) {
                        Log.d(TAG, "Page (re)load started after refresh, attempts=${refreshAttempts.intValue}")
                    }
                    if (!successHandled.value && hasRequiredCookies()) {
                        fireSuccess(context.getString(R.string.login_status_refresh_found))
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    logCurrentUrl(url)
                    lastFinishedUrl.value = url
                    if (!successHandled.value && hasRequiredCookies()) {
                        fireSuccess(context.getString(R.string.login_status_refresh_found))
                    } else if (!successHandled.value && shouldForceRefresh()) {
                        forceRefresh()
                    }
                }

                private fun shouldForceRefresh(): Boolean =
                    hasTokenCookie() &&
                        !hasRefreshTokenCookie() &&
                        lastFinishedUrl.value?.contains(BERANDA_PATH) == true &&
                        refreshAttempts.intValue < MAX_RELOAD_ATTEMPTS

                private fun forceRefresh() {
                    val attempt = refreshAttempts.intValue + 1
                    refreshAttempts.intValue = attempt
                    tokenDetectedAt.longValue = 0L
                    Log.d(
                        TAG,
                        "On $BERANDA_PATH without refresh_token, reloading ($attempt/$MAX_RELOAD_ATTEMPTS)",
                    )
                    onStatus(
                        context.getString(
                            R.string.login_status_reloading_refresh,
                            attempt,
                            MAX_RELOAD_ATTEMPTS,
                        ),
                     )
                    webViewRef.value?.reload()
                    Log.d(TAG, "Web reloaded (attempt $attempt/$MAX_RELOAD_ATTEMPTS)")
                }
            }
        }.also { webViewRef.value = it }
    }

    val loadLoginPage: () -> Unit = {
        successHandled.value = false
        refreshAttempts.intValue = 0
        tokenDetectedAt.longValue = 0L
        lastFinishedUrl.value = null
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        webView.loadUrl(LOGIN_URL)
    }

    LaunchedEffect(webView, reloadTrigger) {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        onStatus(context.getString(R.string.login_status_getting_token))
        loadLoginPage()
        while (!successHandled.value) {
            delay(COOKIE_POLL_INTERVAL_MS)
            if (hasRequiredCookies()) {
                fireSuccess(context.getString(R.string.login_status_refresh_found))
            } else if (hasTokenCookie()) {
                if (tokenDetectedAt.longValue == 0L) {
                    tokenDetectedAt.longValue = System.currentTimeMillis()
                    Log.d(TAG, "Token detected, waiting for refresh_token...")
                    onStatus(context.getString(R.string.login_status_waiting_refresh))
                } else if (System.currentTimeMillis() - tokenDetectedAt.longValue > REFRESH_TOKEN_TIMEOUT_MS) {
                    Log.w(
                        TAG,
                        "Refresh token timeout after $MAX_RELOAD_ATTEMPTS reloads, proceeding with token only",
                    )
                    refreshAttempts.intValue = MAX_RELOAD_ATTEMPTS
                    fireSuccess(context.getString(R.string.login_status_refresh_unavailable))
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())

        if (!successHandled.value) {
            SmallFloatingActionButton(
                onClick = {
                    Log.d(TAG, "Manual refresh tapped")
                    tokenDetectedAt.longValue = 0L
                    refreshAttempts.intValue = 0
                    lastFinishedUrl.value = null
                    webViewRef.value?.reload()
                    Log.d(TAG, "Manual web reload triggered")
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.login_web_refresh),
                )
            }
        }
    }

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
