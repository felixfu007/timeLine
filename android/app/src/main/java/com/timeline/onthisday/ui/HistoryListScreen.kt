package com.timeline.onthisday.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timeline.onthisday.R
import com.timeline.onthisday.data.settings.SortOrder

/**
 * M2 — 歷史事件列表頁 (F-01 display format: 「【西元年份】事件標題」, one row per event).
 * Handles all three [HistoryListUiState] branches explicitly so the screen never renders
 * blank/crashes (F-01 #4).
 *
 * 2026-07-30 user request ("清單列表加入多種排序功能"): [sortOrder]/[onSortOrderSelected] add a
 * sort control to the TopAppBar — see [SortMenuButton] below. The list itself (`uiState.events`)
 * is already sorted by the ViewModel before it reaches this Composable; this screen only renders
 * the current selection and reports taps back up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryListScreen(
    uiState: HistoryListUiState,
    sortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    onEventClick: (HistoryEventUiModel) -> Unit,
    onRetry: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsContentDescription = stringResource(R.string.settings_content_description)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.list_screen_title),
                        fontFamily = englishTitleFontFamilyOrNull()
                    )
                },
                actions = {
                    SortMenuButton(selected = sortOrder, onSelected = onSortOrderSelected)
                    // Plain glyph rather than pulling in material-icons-core for a single icon,
                    // consistent with HistoryDetailScreen's "←" back glyph (M2 precedent).
                    IconButton(onClick = onSettingsClick) {
                        Text(
                            text = "⚙",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics {
                                contentDescription = settingsContentDescription
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is HistoryListUiState.Loading -> LoadingContent()
                is HistoryListUiState.Empty -> EmptyContent(isError = uiState.isError, onRetry = onRetry)
                is HistoryListUiState.Success -> HistoryEventList(
                    events = uiState.events,
                    isStale = uiState.isStale,
                    onEventClick = onEventClick
                )
            }
        }
    }
}

/**
 * TopAppBar sort control (2026-07-30 user request): a plain "⇅" glyph icon (same "no
 * material-icons-core dependency" convention as the "⚙"/"←" glyphs elsewhere in this file/
 * HistoryDetailScreen/SettingsScreen) that opens a [DropdownMenu] listing every [SortOrder]. The
 * currently-active option is marked with a leading "✓" (same low-dependency approach as
 * SettingsScreen's [ColorSwatch] checkmark) so the user can always tell which sort is applied
 * without needing to reopen the menu.
 */
@Composable
private fun SortMenuButton(
    selected: SortOrder,
    onSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val sortContentDescription = stringResource(R.string.sort_content_description)

    Box(modifier = modifier) {
        IconButton(onClick = { menuExpanded = true }) {
            Text(
                text = "⇅",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { contentDescription = sortContentDescription }
            )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            SortOrderMenuItem(
                label = stringResource(R.string.sort_menu_year_ascending),
                isSelected = selected == SortOrder.YEAR_ASCENDING,
                onClick = {
                    menuExpanded = false
                    onSelected(SortOrder.YEAR_ASCENDING)
                }
            )
            SortOrderMenuItem(
                label = stringResource(R.string.sort_menu_year_descending),
                isSelected = selected == SortOrder.YEAR_DESCENDING,
                onClick = {
                    menuExpanded = false
                    onSelected(SortOrder.YEAR_DESCENDING)
                }
            )
        }
    }
}

@Composable
private fun SortOrderMenuItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                // Leading checkmark rather than a trailing Icon composable slot, to mark the
                // active option without depending on material-icons-extended (not a project
                // dependency, same rationale as the "⚙"/"←"/"⇅" glyphs above).
                text = if (isSelected) "✓ $label" else label,
                fontFamily = englishBodyFontFamilyOrNull()
            )
        },
        onClick = onClick
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(isError: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(if (isError) R.string.empty_error_title else R.string.empty_no_data_title),
            fontFamily = englishBodyFontFamilyOrNull(),
            textAlign = TextAlign.Center
        )
        if (isError) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.retry_button),
                    fontFamily = englishBodyFontFamilyOrNull()
                )
            }
        }
    }
}

@Composable
private fun HistoryEventList(
    events: List<HistoryEventUiModel>,
    isStale: Boolean,
    onEventClick: (HistoryEventUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (isStale) {
            Text(
                text = stringResource(R.string.stale_data_notice),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = englishBodyFontFamilyOrNull(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = events, key = { it.id }) { event ->
                HistoryEventRow(event = event, onClick = { onEventClick(event) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryEventRow(
    event: HistoryEventUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 年份數字套用 Cinzel 復古襯線字體（見 YearTypography.kt，不分語言皆套用）；其餘文字在 English 模式
    // 下套用 EB Garamond 內文字體（見 EnglishTypography.kt），繁體中文模式維持系統預設字體不變。年份的
    // SpanStyle 明確指定 Cinzel，會覆蓋掉這裡透過 Text 的 fontFamily 參數設定的「基底」字體，兩者互不衝突。
    val fullText = stringResource(R.string.event_item_format, event.year, event.displayText)
    Text(
        text = buildYearHighlightedText(
            fullText = fullText,
            year = event.year,
            yearStyle = SpanStyle(fontFamily = CinzelFontFamily, fontWeight = FontWeight.Bold)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = englishBodyFontFamilyOrNull()
        // No maxLines/overflow truncation here: unlike the Widget (F-01 #3 requires "…"
        // truncation), the in-app list has room for long titles to wrap onto multiple lines.
    )
}
