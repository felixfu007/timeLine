# timeLine

**歷史上的今天 — 跑馬燈 Widget APP**

每天在手機主畫面的 Widget 上，以跑馬燈形式自動顯示「歷史上的今天」重大事件。

## 專案狀態

🟡 **設計階段** — 尚未開始開發，正在評估技術方案。

## 文件

- 📋 [專案需求書（REQUIREMENTS.md）](./REQUIREMENTS.md) — 包含多個實作方案、套件選型、資料來源比較及技術建議

## 評估方案摘要

| 方案 | 技術 | 適用情境 |
|------|------|----------|
| 方案一 | Flutter + home_widget | 跨平台（iOS + Android），單一程式碼庫 |
| 方案二 | Android 原生 Kotlin + Jetpack Glance | 僅 Android，Widget 功能最完整 |
| 方案三 | iOS 原生 Swift + WidgetKit | 僅 iOS，iOS Widget 最標準做法 |
| 方案四 | React Native | 跨平台，但 Widget 套件成熟度低（不建議） |

詳細比較請參閱 [REQUIREMENTS.md](./REQUIREMENTS.md)。
