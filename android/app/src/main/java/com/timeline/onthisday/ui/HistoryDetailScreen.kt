package com.timeline.onthisday.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timeline.onthisday.R

/**
 * M2 — minimal event detail screen (F-06 groundwork): full event text + year, plus source
 * attribution when available, plus the CC BY-SA notice (ANALYSIS.md 6.2 待辦事項).
 *
 * Not wired to real Wikipedia `pages[].extract` summaries beyond title+URL — see
 * HistoryEventEntity's sourceTitle/sourceUrl fields (M2 schema addition); the full "extract"
 * snippet is not persisted, only the event's own `text` (already shown as [event.displayText]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    event: HistoryEventUiModel?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.detail_screen_title),
                        fontFamily = englishTitleFontFamilyOrNull()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Plain glyph instead of Material Icons to avoid pulling in the
                        // material-icons-core artifact for a single icon in M2 — proper iconography
                        // is UI-polish scope (M6).
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 4.dp)
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
            if (event == null) {
                Text(
                    text = stringResource(R.string.detail_not_found),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    fontFamily = englishBodyFontFamilyOrNull(),
                    textAlign = TextAlign.Center
                )
            } else {
                HistoryDetailContent(event = event, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun HistoryDetailContent(event: HistoryEventUiModel, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // 年份數字套用 Cinzel 復古襯線字體（見 YearTypography.kt，不分語言皆套用）；其餘內文（標籤文字、
        // 事件全文、來源標註）在 English 模式下套用 EB Garamond（見 EnglishTypography.kt），繁體中文模式
        // 維持系統預設字體不變。
        val bodyFontFamily = englishBodyFontFamilyOrNull()
        Text(
            text = buildYearHighlightedText(
                fullText = stringResource(R.string.detail_year_label, event.year),
                year = event.year,
                yearStyle = SpanStyle(fontFamily = CinzelFontFamily, fontWeight = FontWeight.Bold)
            ),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = bodyFontFamily,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = event.displayText,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = bodyFontFamily
        )

        if (event.sourceTitle != null) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.source_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = bodyFontFamily
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.sourceTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = bodyFontFamily,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (event.sourceUrl != null) {
                    Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(event.sourceUrl) }
                } else {
                    Modifier.fillMaxWidth()
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.attribution_notice),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = bodyFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
