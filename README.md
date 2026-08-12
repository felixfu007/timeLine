# timeLine

**歷史上的今天 — 跑馬燈 Widget APP**

每天在手機主畫面的 Widget 上，以定時輪播形式自動顯示「歷史上的今天」重大事件，資料來自 Wikipedia（CC BY-SA 授權）。

## 專案狀態

🟢 **Android 版開發中（M1–M7 已完成）** — 主 APP（列表／詳情頁、排序、Widget 外觀自訂）、Glance Widget 輪播、每日背景抓取、離線快取、中英文介面皆已實作並經過真機測試。下一步是 M8（Google Play 上架準備）。

## 下載

📦 **[下載目前版本 APK（v0.8.0）](./release/onthisday-widget-v0.8.0.apk)**

這是尚未上架 Google Play 的測試用簽署版 APK，僅供直接安裝（sideload）。安裝方式：

1. 用手機瀏覽器打開上面的下載連結，或用 USB 傳輸到手機後點擊安裝。
2. 若系統跳出「不明來源」／「安裝未知應用程式」警告，需手動允許該來源安裝（Android 設定 → 安全性）。
3. 安裝完成後，長按主畫面空白處 → 小工具 → 找到「On This Day / 歷史上的今天」拖曳到主畫面。

> 目前只有 Android 版；iOS 版未列入本階段開發範圍（見 [REQUIREMENTS.md](./REQUIREMENTS.md) 方案評估）。

## 文件

- 📋 [專案需求書（REQUIREMENTS.md）](./REQUIREMENTS.md) — 功能需求、資料來源與技術方案比較
- 🔍 [技術選型分析（ANALYSIS.md）](./ANALYSIS.md) — 選型決策、驗收條件、風險與里程碑覆核
- ✅ [M7 測試報告（android/QA_REPORT_M7.md）](./android/QA_REPORT_M7.md) — 功能／非功能驗收、缺陷與修復紀錄

## 技術方案

Android 原生：Kotlin + Jetpack Compose + Jetpack Glance（Widget）+ Room（本機快取）+ Retrofit（Wikipedia On This Day API）+ WorkManager（背景排程）+ Hilt（DI）。詳細理由見 [ANALYSIS.md](./ANALYSIS.md)。

## 開發流程

專案採用系統分析師／開發工程師／QA 測試員三個角色分工（見 [.claude/agents/](./.claude/agents/)）。每次要發佈新版本時，遵循 `release` skill 的固定流程（簽署 APK → 更新 `release/` 資料夾 → 更新本文件下載連結 → 建立 git tag）。
