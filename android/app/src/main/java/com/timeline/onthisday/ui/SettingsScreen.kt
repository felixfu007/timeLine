package com.timeline.onthisday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timeline.onthisday.R
import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.data.settings.AppSettings
import com.timeline.onthisday.data.settings.WidgetBackgroundColor
import com.timeline.onthisday.data.settings.WidgetBorderColor
import com.timeline.onthisday.data.settings.WidgetTextColor

/**
 * M6 — settings screen (F-05 rotation interval, F-07 language, Widget appearance colors). A plain
 * single-selection radio list per section rather than a dropdown, since there are only 2/3 options
 * each — keeps every option visible/tappable without an extra open/close interaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AppSettings,
    onLanguageSelected: (AppLanguage) -> Unit,
    onRotationIntervalSelected: (Int) -> Unit,
    onWidgetBackgroundColorSelected: (WidgetBackgroundColor) -> Unit,
    onWidgetTextColorSelected: (WidgetTextColor) -> Unit,
    onWidgetBorderColorSelected: (WidgetBorderColor) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        fontFamily = englishTitleFontFamilyOrNull()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle(stringResource(R.string.settings_language_section_title))
            LanguageOptions(selected = uiState.language, onSelected = onLanguageSelected)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_rotation_interval_section_title))
            Text(
                text = stringResource(R.string.settings_rotation_interval_hint),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = englishBodyFontFamilyOrNull(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            RotationIntervalOptions(
                selectedMinutes = uiState.rotationIntervalMinutes,
                onSelected = onRotationIntervalSelected
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_widget_appearance_section_title))
            WidgetAppearancePreview(
                backgroundColor = uiState.widgetBackgroundColor,
                textColor = uiState.widgetTextColor,
                borderColor = uiState.widgetBorderColor
            )
            WidgetColorSection(
                label = stringResource(R.string.settings_widget_background_color_label),
                options = WidgetBackgroundColor.entries,
                colorOf = { Color(it.argb) },
                nameOf = { backgroundColorName(it) },
                selected = uiState.widgetBackgroundColor,
                onSelected = onWidgetBackgroundColorSelected
            )
            WidgetColorSection(
                label = stringResource(R.string.settings_widget_text_color_label),
                options = WidgetTextColor.entries,
                colorOf = { Color(it.argb) },
                nameOf = { textColorName(it) },
                selected = uiState.widgetTextColor,
                onSelected = onWidgetTextColorSelected
            )
            WidgetColorSection(
                label = stringResource(R.string.settings_widget_border_color_label),
                options = WidgetBorderColor.entries,
                colorOf = { Color(it.argb) },
                nameOf = { borderColorName(it) },
                selected = uiState.widgetBorderColor,
                onSelected = onWidgetBorderColorSelected
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    // Section labels (e.g. "Language" / "Widget appearance") are body-level content within the
    // Settings screen, not the screen title itself — only the TopAppBar title gets Cinzel.
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = englishBodyFontFamilyOrNull(),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun LanguageOptions(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.selectableGroup()) {
        AppLanguage.entries.forEach { language ->
            val label = when (language) {
                AppLanguage.ZH_HANT -> stringResource(R.string.settings_language_zh_hant)
                AppLanguage.EN -> stringResource(R.string.settings_language_en)
            }
            RadioOptionRow(
                label = label,
                selected = language == selected,
                onClick = { onSelected(language) }
            )
        }
    }
}

@Composable
private fun RotationIntervalOptions(
    selectedMinutes: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.selectableGroup()) {
        AppSettings.ROTATION_INTERVAL_OPTIONS_MINUTES.forEach { minutes ->
            RadioOptionRow(
                label = stringResource(R.string.settings_rotation_interval_option, minutes),
                selected = minutes == selectedMinutes,
                onClick = { onSelected(minutes) }
            )
        }
    }
}

@Composable
private fun RadioOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = englishBodyFontFamilyOrNull(),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/**
 * Live sample card showing the currently-selected background/text/border combination — mirrors
 * the actual Widget card's rounded-corner + thin-border look (widget/HistoryWidget.kt's
 * [androidx.glance.appwidget.cornerRadius] + layered-Box border technique), just rendered with
 * plain Compose (`Modifier.border`/`clip`) since this runs in the regular App UI, not Glance.
 */
@Composable
private fun WidgetAppearancePreview(
    backgroundColor: WidgetBackgroundColor,
    textColor: WidgetTextColor,
    borderColor: WidgetBorderColor,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.settings_widget_appearance_preview_label),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = englishBodyFontFamilyOrNull(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(borderColor.argb))
                .padding(2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(backgroundColor.argb))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_widget_appearance_preview_text),
                color = Color(textColor.argb),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = englishBodyFontFamilyOrNull(),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** One labeled row of tappable color-dot swatches (background/text/border each get one of these). */
@Composable
private fun <T> WidgetColorSection(
    label: String,
    options: List<T>,
    colorOf: (T) -> Color,
    nameOf: @Composable (T) -> String,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = englishBodyFontFamilyOrNull(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                ColorSwatch(
                    color = colorOf(option),
                    selected = option == selected,
                    description = nameOf(option),
                    onClick = { onSelected(option) }
                )
            }
        }
    }
}

/**
 * A single tappable color dot. The selected swatch gets a visibly thicker outer ring in the
 * theme's primary color plus a small checkmark drawn in whichever of black/white contrasts best
 * against that swatch's own fill — this is the "明顯的選取標記" the appearance settings UI needs
 * (2026-07-30 user UX request), without depending on the material-icons-extended artifact (not a
 * project dependency) for a checkmark glyph.
 */
@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringWidth = if (selected) 3.dp else 1.dp
    val ringColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .size(40.dp)
            .border(width = ringWidth, color = ringColor, shape = CircleShape)
            .padding(if (selected) 4.dp else 3.dp)
            .clip(CircleShape)
            .background(color)
            .selectable(selected = selected, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text(
                text = "✓",
                color = if (color.luminance() > 0.5f) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun backgroundColorName(color: WidgetBackgroundColor): String = stringResource(
    when (color) {
        WidgetBackgroundColor.LAVENDER -> R.string.settings_widget_color_lavender
        WidgetBackgroundColor.PINK -> R.string.settings_widget_color_pink
        WidgetBackgroundColor.BLUE -> R.string.settings_widget_color_blue
        WidgetBackgroundColor.GREEN -> R.string.settings_widget_color_green
        WidgetBackgroundColor.YELLOW -> R.string.settings_widget_color_yellow
        WidgetBackgroundColor.WHITE -> R.string.settings_widget_color_white
        WidgetBackgroundColor.GRAY -> R.string.settings_widget_color_gray
    }
)

@Composable
private fun textColorName(color: WidgetTextColor): String = stringResource(
    when (color) {
        WidgetTextColor.DARK_GRAY -> R.string.settings_widget_color_dark_gray
        WidgetTextColor.DARK_PURPLE -> R.string.settings_widget_color_dark_purple
        WidgetTextColor.DARK_BLUE -> R.string.settings_widget_color_dark_blue
        WidgetTextColor.DARK_BROWN -> R.string.settings_widget_color_dark_brown
    }
)

@Composable
private fun borderColorName(color: WidgetBorderColor): String = stringResource(
    when (color) {
        WidgetBorderColor.SOFT_PURPLE -> R.string.settings_widget_color_soft_purple
        WidgetBorderColor.SOFT_GRAY -> R.string.settings_widget_color_soft_gray
        WidgetBorderColor.SOFT_BLUE -> R.string.settings_widget_color_soft_blue
        WidgetBorderColor.SOFT_GOLD -> R.string.settings_widget_color_soft_gold
    }
)
