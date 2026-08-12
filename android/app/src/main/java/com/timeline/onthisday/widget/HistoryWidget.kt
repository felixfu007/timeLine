package com.timeline.onthisday.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.timeline.onthisday.MainActivity
import com.timeline.onthisday.R
import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.data.settings.WidgetBackgroundColor
import com.timeline.onthisday.data.settings.WidgetBorderColor
import com.timeline.onthisday.data.settings.WidgetTextColor
import com.timeline.onthisday.di.HistoryWidgetEntryPoint
import com.timeline.onthisday.ui.HistoryEventUiModel
import com.timeline.onthisday.ui.toUiModels
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate

/**
 * M3 — Widget 輪播顯示 (F-01/F-03/F-06).
 *
 * `provideGlance` does a one-shot read of whatever is currently cached in Room (never a network
 * call — that's WidgetRotationWorker's job) and renders the single event at the widget
 * instance's current rotation index. Content only changes when something calls `updateAll()`
 * (WidgetRotationWorker on its periodic tick, or the system on first placement) — Glance widgets
 * don't recompose live off a background Flow, so this deliberately uses one-shot suspend reads
 * rather than `collectAsState` for simplicity and to avoid redundant recompositions.
 *
 * Deliberately a plain no-arg class (not constructor-injected, not `@AndroidEntryPoint` on the
 * receiver either) — DAOs are looked up via [HistoryWidgetEntryPoint] inside `provideGlance`
 * itself. See that entry point's kdoc for why: Glance's `GlanceAppWidgetManager` sometimes
 * instantiates [HistoryWidgetReceiver] directly via reflection outside Hilt's control, which
 * broke an earlier constructor-injection design (confirmed by a real crash during M3 emulator
 * verification — `UninitializedPropertyAccessException` on a Hilt-injected DAO field).
 */
class HistoryWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SIZE_4X1, SIZE_4X2))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, HistoryWidgetEntryPoint::class.java)
        val historyEventDao = entryPoint.historyEventDao()
        val widgetStateDao = entryPoint.widgetStateDao()
        val settingsRepository = entryPoint.settingsRepository()

        val widgetId = GlanceAppWidgetManager(appContext).getAppWidgetId(id)

        // F-07 AC2 / M7 defect #1 fix: reads the DataStore language setting directly (the
        // Widget process doesn't recompose off AppCompatDelegate's per-app locale — that
        // mechanism only patches Activity Contexts) and uses it both to pick which language's
        // independently-cached event list to display (zh and en are separate curated feeds, not
        // translations of each other — see HistoryEventEntity's kdoc) and to resolve this
        // composable's one dynamic string against a locale-overridden Context.
        //
        // 2026-07-30 (Widget 外觀自訂): the appearance colors below are read the same one-shot way
        // as language — they take effect the next time something calls `updateAll()`, either
        // WidgetRotationWorker's periodic tick or (for these color settings specifically, unlike
        // language) an immediate on-demand refresh triggered right after the user picks a new
        // color — see widget/WidgetRefresher.kt's kdoc.
        val settings = settingsRepository.currentSettings()
        val language = settings.language

        val today = LocalDate.now()
        val events = historyEventDao
            .getEventsForDateOnce(today.monthValue, today.dayOfMonth, language.name)
            .toUiModels()
        val storedIndex = widgetStateDao.getState(widgetId)?.currentIndex
        val currentEvent = if (events.isEmpty()) {
            null
        } else {
            events[WidgetRotationState.currentIndexOrFallback(storedIndex, events.size)]
        }

        val preparingDataText = appContext.withAppLocale(language).getString(R.string.widget_preparing_data)

        provideContent {
            GlanceTheme {
                HistoryWidgetContent(
                    event = currentEvent,
                    emptyStateText = preparingDataText,
                    backgroundColor = settings.widgetBackgroundColor,
                    textColor = settings.widgetTextColor,
                    borderColor = settings.widgetBorderColor
                )
            }
        }
    }

    companion object {
        /** Roughly a 4x1 home-screen cell area (title-only layout). */
        val SIZE_4X1 = DpSize(180.dp, 40.dp)

        /**
         * Roughly a 4x2 home-screen cell area (title + year sub-line layout). Shortened from the
         * original 110dp per a user UX request (2026-07-29): the widget felt unnecessarily tall,
         * and Android Widgets can't do a real marquee animation anyway (ANALYSIS.md 6.1), so
         * there's no benefit to reserving extra vertical space beyond what 2-3 lines of compact
         * text actually needs. Kept in sync with `res/xml/history_widget_info.xml`'s
         * `maxResizeHeight`.
         */
        val SIZE_4X2 = DpSize(250.dp, 80.dp)

        val EVENT_ID_KEY = ActionParameters.Key<Long>("eventId")
    }
}

/**
 * Returns a [Context] whose resources resolve against [language]'s locale, regardless of the
 * device/App's actual current locale (F-07 AC2 — see [HistoryWidget.provideGlance]'s kdoc for
 * why the widget can't just rely on `AppCompatDelegate`/system locale here).
 */
private fun Context.withAppLocale(language: AppLanguage): Context {
    val locale = language.toJavaLocale()
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

/** The visible border ring's thickness (2026-07-30 — see [HistoryWidgetContent]'s kdoc). */
private val BORDER_WIDTH = 2.dp

/** Outer (border box) corner radius. Applied to both the 4x1 and 4x2 layouts. */
private val OUTER_CORNER_RADIUS = 16.dp

/**
 * Inner (background box) corner radius — deliberately [OUTER_CORNER_RADIUS] minus [BORDER_WIDTH]
 * rather than reusing the outer radius, so the border ring reads as a uniform-thickness stroke
 * around the curve instead of the inner box's corner poking past the outer box's corner.
 */
private val INNER_CORNER_RADIUS = OUTER_CORNER_RADIUS - BORDER_WIDTH

/**
 * 字體美化（2026-07-30）：年份數字在 App 內（列表/詳情頁）已改用 Cinzel 復古襯線字體
 * （見 `ui/YearTypography.kt`），但 Widget 端**刻意維持系統預設字體**，原因是已查證的平台限制：
 *
 * `androidx.glance.text.TextStyle.fontFamily` 雖然型別上接受任意 `FontFamily(String)`，但反編譯
 * Glance 1.1.1 (`glance-appwidget-1.1.1`) 的 `TextTranslatorKt`（RemoteViews 轉譯層）後確認，它是用
 * `android.text.style.TypefaceSpan(String)`（僅能以「系統已知的字型家族名稱」如 "sans-serif"／
 * "serif" 查找 `Typeface.create(name, style)`）這個舊版建構子來套用字體，而不是 API 28+ 才有、可以
 * 直接帶入一個實際 `Typeface` 物件（例如從 `res/font` 載入的自訂 ttf）的 `TypefaceSpan(Typeface)`
 * 建構子。也就是說，即使在這裡塞入 `FontFamily("Cinzel")`，RemoteViews 底層也只會嘗試以字串
 * "Cinzel" 去比對系統內建字型名稱，查無結果、靜默 fallback 回系統預設字體，不會真的套用到
 * `res/font/cinzel_*.ttf`——Glance 目前沒有提供載入自訂字體資源給 Widget 使用的 API。
 *
 * 因此本次不在 Widget 硬做（例如把文字轉成 Bitmap 手繪自訂字體再塞進 `RemoteViews.setImageViewBitmap`
 * ——這正是 ANALYSIS.md 6.1 節明確建議不要採用的做法，會觸發更新頻率節流與額外耗電，得不償失），
 * 僅記錄為已知限制。若未來 Glance 釋出支援自訂字體資源的 API，可在此補上與 App 內一致的 Cinzel 套用。
 */
@Composable
private fun HistoryWidgetContent(
    event: HistoryEventUiModel?,
    emptyStateText: String,
    backgroundColor: WidgetBackgroundColor,
    textColor: WidgetTextColor,
    borderColor: WidgetBorderColor
) {
    val size = LocalSize.current
    val isWideLayout = size.height >= HistoryWidget.SIZE_4X2.height

    // 2026-07-30 user UX request: the widget background used to be a `fillMaxSize()` card that
    // filled the *entire* widget grid area, including any extra blank space a user drags the
    // widget into on the home screen (Android Widgets are freely resizable). That made the empty
    // area look like an opaque block sitting on top of the wallpaper. Fix: split into two layers.
    // The outer Box stays fully transparent and `fillMaxSize()` — it just defines the tap target
    // and positions the card within whatever size the user resized the widget to. The inner boxes
    // are `wrapContentSize()` so the visible card hugs the text content instead of stretching to
    // fill unused space.
    val outerModifier = GlanceModifier.fillMaxSize().let { base ->
        if (event != null) {
            base.clickable(
                actionStartActivity<MainActivity>(
                    parameters = actionParametersOf(HistoryWidget.EVENT_ID_KEY to event.id)
                )
            )
        } else {
            base
        }
    }

    val textColorProvider = ColorProvider(Color(textColor.argb))

    // 2026-07-30 user UX request: a rounded card with a thin stroke border, in one of a preset set
    // of user-selected colors (data/settings/WidgetColorPalette.kt). Glance 1.1.1 has
    // `GlanceModifier.cornerRadius(Dp)` (androidx.glance.appwidget.CornerRadiusKt) for real rounded
    // corners, but no `Modifier.border()`-equivalent for a stroke-only outline — so the border is
    // faked with the standard "two nested boxes, offset by the border width" trick: [borderModifier]
    // paints the outer box in the border color, then [cardModifier]'s `padding(BORDER_WIDTH)` on
    // the outer box leaves exactly a [BORDER_WIDTH]-wide ring of the outer color visible around the
    // inner, actual-background-colored box. Applied identically to both the 4x1 and 4x2 layouts
    // below (both share this single set of modifiers).
    val borderModifier = GlanceModifier
        .wrapContentSize()
        .background(ColorProvider(Color(borderColor.argb)))
        .cornerRadius(OUTER_CORNER_RADIUS)
        .padding(BORDER_WIDTH)

    // Vertical content padding trimmed to 6dp (was a uniform 12dp) per a 2026-07-29 user UX
    // request: the widget — especially the 4x2 layout — felt too tall/loose. Horizontal padding is
    // left at 12dp since the width complaint was never raised; only vertical whitespace is tightened.
    val cardModifier = GlanceModifier
        .wrapContentSize()
        .background(ColorProvider(Color(backgroundColor.argb)))
        .cornerRadius(INNER_CORNER_RADIUS)
        .padding(horizontal = 12.dp, vertical = 6.dp)

    Box(modifier = outerModifier, contentAlignment = Alignment.CenterStart) {
        Box(modifier = borderModifier, contentAlignment = Alignment.CenterStart) {
            Box(modifier = cardModifier, contentAlignment = Alignment.CenterStart) {
                when {
                    event == null -> Text(
                        text = emptyStateText,
                        maxLines = 2,
                        style = TextStyle(color = textColorProvider)
                    )

                    isWideLayout -> Column {
                        // Explicit (slightly smaller than Glance's ~16sp default) font sizes to flatten
                        // this two-line layout's overall height, per the same 2026-07-29 request as the
                        // padding change above — still comfortably readable, just less loose than default.
                        Text(
                            text = event.year.toString(),
                            style = TextStyle(
                                color = textColorProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = event.displayText,
                            maxLines = 2,
                            style = TextStyle(color = textColorProvider, fontSize = 13.sp)
                        )
                    }

                    else -> Text(
                        // F-01 #1/#3: 「【西元年份】事件標題」, single line, truncated by Glance's default
                        // ellipsis-on-overflow text handling in the narrow 4x1 layout.
                        text = "【${event.year}】${event.displayText}",
                        maxLines = 1,
                        style = TextStyle(color = textColorProvider)
                    )
                }
            }
        }
    }
}
