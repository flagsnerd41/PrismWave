package com.prismwave.app

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pull down on Home and let go: a fresh set of songs, with a small spinner. The layout inside is unchanged. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefreshHome(s: AppState, content: @Composable BoxScope.() -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + PinnedHeaderHeight
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                if (!Opts.offline) {
                    Fresh.refresh(ctx, s, true)
                    s.reloadYtHome()
                    delay(500)
                    var n = 0
                    while (Fresh.busy && n < 60) {
                        delay(150)
                        n++
                    }
                } else {
                    delay(500)
                }
                refreshing = false
            }
        },
        modifier = Modifier.fillMaxSize(),
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = top),
                containerColor = CardBg2,
                color = s.accent
            )
        },
        content = content
    )
}
