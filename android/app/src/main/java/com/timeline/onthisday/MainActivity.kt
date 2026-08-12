package com.timeline.onthisday

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timeline.onthisday.ui.AppDestinations
import com.timeline.onthisday.ui.HistoryDetailScreen
import com.timeline.onthisday.ui.HistoryDetailViewModel
import com.timeline.onthisday.ui.HistoryListScreen
import com.timeline.onthisday.ui.HistoryListViewModel
import com.timeline.onthisday.ui.SettingsScreen
import com.timeline.onthisday.ui.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * M2 — 主 APP 歷史事件列表頁 + 簡易事件詳情頁, hosted via Compose Navigation (two destinations).
 *
 * M3 (F-06): tapping the currently-shown event on the Widget launches this Activity with an
 * `eventId` extra (see widget/HistoryWidget.kt's `actionStartActivity`); when present, the
 * NavHost pushes the detail screen for that event on top of the list screen (see
 * [OnThisDayNavHost]'s kdoc — M7 defect #2 fix).
 *
 * Post-M7 user-reported defect fix: re-tapping the Widget while the app is already backgrounded
 * (not force-stopped) used to stack a *second* `MainActivity` instance on top of the existing one
 * in the same Task (confirmed via `dumpsys activity activities`, `numActivities=2`), leaving the
 * back button unable to get past the stale hidden instance underneath. Fixed with
 * `android:launchMode="singleTop"` (AndroidManifest.xml) so the system redelivers the Intent to
 * the existing top instance via [onNewIntent] instead of creating a new one, combined with
 * [pendingWidgetNavigation] — a `mutableStateOf` read by [OnThisDayNavHost] — so both the
 * `onCreate` path (cold start / new Task) and the `onNewIntent` path (Widget tapped while this
 * Activity is already on top) drive the exact same "push detail route" navigation effect. Known
 * remaining gap: if `MainActivity` exists in the Task but is *not* currently at the top (e.g. the
 * user navigated to another app and back without MainActivity being killed), `singleTop` does not
 * apply and the system still creates a new instance — Glance's `actionStartActivity` does not
 * currently expose a way to attach `FLAG_ACTIVITY_CLEAR_TOP`/`FLAG_ACTIVITY_SINGLE_TOP` to the
 * Intent it builds, so this more marginal scenario is left as a known limitation rather than
 * blocking this fix (see QA_REPORT_M7.md 十).
 *
 * M6: adds a third destination, [AppDestinations.SETTINGS_ROUTE] (language F-07, rotation
 * interval F-05; category filter F-08 UI skeleton is out of scope for M6, see ANALYSIS.md 四
 * F-08). Extends [AppCompatActivity] (rather than the plain `ComponentActivity` used through
 * M2/M3) so `AppCompatDelegate.setApplicationLocales()` (see TimelineApplication /
 * SettingsViewModel) can automatically recreate this Activity when the user changes the app
 * language, re-rendering Compose with the new locale's string resources (F-07 AC2, "App 內 UI
 * 字串...立即套用") — `setContent`/Compose Navigation work unchanged under AppCompatActivity since
 * it's still a `ComponentActivity` subclass.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /**
     * The most recent Widget-originated navigation request (or `null` if this Activity was
     * launched without one, e.g. a plain launcher icon tap), exposed as Compose state so
     * [OnThisDayNavHost] reacts to it from both [onCreate] and [onNewIntent].
     */
    private var pendingWidgetNavigation by mutableStateOf<WidgetNavigationRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingWidgetNavigation = intent?.toWidgetNavigationRequest()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OnThisDayNavHost(
                        pendingNavigation = pendingWidgetNavigation,
                        onPendingNavigationHandled = { pendingWidgetNavigation = null }
                    )
                }
            }
        }
    }

    /**
     * Invoked instead of a fresh [onCreate] when `singleTop` finds this Activity already at the
     * top of its Task (see this class's kdoc). Updates [pendingWidgetNavigation] with a *new*
     * [WidgetNavigationRequest] (unique `requestId` per tap, even for the same `eventId`) so
     * [OnThisDayNavHost]'s `LaunchedEffect` fires again and pushes the detail route for whichever
     * event was just tapped on the Widget — including the edge case of tapping the same
     * currently-displayed event twice in a row.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetNavigation = intent.toWidgetNavigationRequest()
    }

    private fun Intent.toWidgetNavigationRequest(): WidgetNavigationRequest? {
        val eventId = getLongExtra(AppDestinations.EVENT_ID_ARG, NO_EVENT_ID).takeIf { it != NO_EVENT_ID }
            ?: return null
        return WidgetNavigationRequest(eventId = eventId, requestId = System.nanoTime())
    }

    private companion object {
        const val NO_EVENT_ID = -1L
    }
}

/**
 * A single Widget-tap-originated navigation request. [requestId] exists purely so
 * [OnThisDayNavHost]'s `LaunchedEffect` (keyed on this whole object, via its `equals`/`hashCode`
 * from being a `data class`) can tell "tap event #111 again" apart from "still showing the result
 * of the previous tap on event #111" — a plain `eventId: Long` key would not re-fire the effect on
 * a repeat tap of the same event.
 */
private data class WidgetNavigationRequest(val eventId: Long, val requestId: Long)

/**
 * M7 defect #2 fix: [AppDestinations.LIST_ROUTE] is now *always* the NavHost's start destination,
 * even when opened via a Widget tap. Previously, when a Widget-originated event id was present,
 * the detail route was set directly as `startDestination`, meaning the list screen was never
 * pushed onto the back stack — `HistoryDetailScreen`'s "←" button called `popBackStack()`, which
 * silently no-oped because there was nothing to pop back to (a dead button the user couldn't tell
 * was broken). Instead, a [LaunchedEffect] pushes the detail route *on top of* the list route
 * whenever [pendingNavigation] changes, so the back stack is always `[List, Detail]` in that case
 * — `onBack`'s existing `popBackStack()` call then genuinely returns to the list screen.
 *
 * Post-M7 fix: [pendingNavigation] is no longer a one-shot constructor-time value — it's
 * [MainActivity]'s Compose state, updated from both `onCreate` (cold start) and `onNewIntent`
 * (Widget tapped while this Activity is already on top of its Task, see [MainActivity]'s kdoc),
 * so the effect below re-fires on every new Widget tap, not just the first one. [onPendingNavigationHandled]
 * clears the pending request after navigating so a later recomposition (e.g. a language-change
 * `Activity` recreate) doesn't re-navigate to the same detail screen the user may have already
 * backed out of.
 *
 * The `navigate` call below uses `popUpTo(LIST_ROUTE) { inclusive = false }` +
 * `launchSingleTop = true`. Discovered while manually verifying the `onNewIntent` fix above on a
 * real device: without this, tapping the Widget a second time while already showing a (possibly
 * stale) detail screen simply pushed *another* `Detail` entry on top instead of replacing it, so
 * the back stack grew unboundedly (`[List, Detail, Detail, ...]`) — same symptom as M7 defect #2
 * (back button feels like it "does nothing" the first time or two) but caused by the nav graph's
 * back stack rather than a duplicate Activity instance. `popUpTo(LIST_ROUTE, inclusive = false)`
 * collapses any existing Detail (or Settings) screens back down to List before pushing the new
 * Detail, so the stack is always exactly `[List, Detail]` no matter how many times the Widget is
 * tapped in a row — back always takes exactly two presses to fully exit, matching a normal
 * List→Detail tap from within the app.
 */
@Composable
private fun OnThisDayNavHost(
    pendingNavigation: WidgetNavigationRequest?,
    onPendingNavigationHandled: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppDestinations.LIST_ROUTE) {
        composable(AppDestinations.LIST_ROUTE) {
            val viewModel: HistoryListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

            HistoryListScreen(
                uiState = uiState,
                sortOrder = sortOrder,
                onSortOrderSelected = viewModel::setSortOrder,
                onEventClick = { event -> navController.navigate(AppDestinations.detailRoute(event.id)) },
                onRetry = viewModel::retry,
                onSettingsClick = { navController.navigate(AppDestinations.SETTINGS_ROUTE) }
            )
        }

        composable(
            route = AppDestinations.DETAIL_ROUTE,
            arguments = listOf(navArgument(AppDestinations.EVENT_ID_ARG) { type = NavType.LongType })
        ) {
            val viewModel: HistoryDetailViewModel = hiltViewModel()
            val event by viewModel.uiState.collectAsStateWithLifecycle()

            HistoryDetailScreen(
                event = event,
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppDestinations.SETTINGS_ROUTE) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            SettingsScreen(
                uiState = uiState,
                onLanguageSelected = viewModel::setLanguage,
                onRotationIntervalSelected = viewModel::setRotationIntervalMinutes,
                onWidgetBackgroundColorSelected = viewModel::setWidgetBackgroundColor,
                onWidgetTextColorSelected = viewModel::setWidgetTextColor,
                onWidgetBorderColorSelected = viewModel::setWidgetBorderColor,
                onBack = { navController.popBackStack() }
            )
        }
    }

    LaunchedEffect(pendingNavigation) {
        if (pendingNavigation != null) {
            navController.navigate(AppDestinations.detailRoute(pendingNavigation.eventId)) {
                popUpTo(AppDestinations.LIST_ROUTE) { inclusive = false }
                launchSingleTop = true
            }
            onPendingNavigationHandled()
        }
    }
}
