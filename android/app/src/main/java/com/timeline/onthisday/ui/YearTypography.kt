package com.timeline.onthisday.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.timeline.onthisday.R

/**
 * UI 美化（2026-07-30）：年份數字改用復古西文襯線字體 Cinzel（Google Fonts，SIL OFL 1.1 授權，
 * 可自由嵌入 App 免費使用），僅套用於「年份數字」本身，中文事件內容一律維持系統預設字體不動，
 * 理由：
 *  1) Cinzel 為西文字型，不含中文字符字型，若對整段中文文字套用會被系統字型 fallback 掩蓋，沒有效果、
 *     反而增加 APK 體積卻無實際視覺變化；
 *  2) Cinzel 屬於仿古羅馬石刻銘文風格的等粗筆畫襯線體，比起高對比襯線體（如 Playfair Display）在小尺寸
 *     下更清楚易讀，經實測比較（見開發交付說明）後選用，適合 Widget 4×1/4×2 這種小字級場景延伸使用。
 *
 * 只需 Regular／Bold 兩個字重（詳情頁年份用 Bold 強調，列表頁年份沿用 Normal 搭配列表本身字級即可）。
 */
val CinzelFontFamily: FontFamily = FontFamily(
    Font(R.font.cinzel_regular, FontWeight.Normal),
    Font(R.font.cinzel_bold, FontWeight.Bold)
)

/**
 * 將 [fullText]（已依語言 format 好的完整字串，例如「【1969】阿姆斯壯登陸月球」或「西元 1969 年」／
 * 「Year 1969」）中對應 [year] 的數字子字串，套用 [yearStyle]（Cinzel 字體），其餘文字維持預設字體。
 *
 * 用字串比對（找出 `year.toString()` 在 [fullText] 中的位置）而非硬編碼版面位置，是因為 format 字串
 * 本身依語言（zh/en）不同，且刻意不重複維護一份「中括號/年份/內文」拆分邏輯——只要目前或未來的字串資源
 * 仍然把年份數字以一般十進位整數（不含千分位逗號）方式塞進格式化字串，這裡就能正確定位、不受语系差異影響。
 * 若找不到（理論上不會發生，除非未來格式改成千分位或全形數字），則整段退回預設字體，不崩潰、不誤植樣式。
 */
fun buildYearHighlightedText(
    fullText: String,
    year: Int,
    yearStyle: SpanStyle
): AnnotatedString {
    val yearDigits = year.toString()
    val startIndex = fullText.indexOf(yearDigits)
    return buildAnnotatedString {
        if (startIndex < 0) {
            append(fullText)
        } else {
            append(fullText.substring(0, startIndex))
            withStyle(yearStyle) {
                append(yearDigits)
            }
            append(fullText.substring(startIndex + yearDigits.length))
        }
    }
}
