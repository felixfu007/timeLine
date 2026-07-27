# 專案需求書：歷史上的今天跑馬燈 Widget APP

> 版本：v1.0（設計階段）  
> 日期：2026-07-27  
> 狀態：評估中，尚未開始開發

---

## 一、專案概述

開發一套行動裝置 APP，每天在手機主畫面的 **Widget（小工具）** 上，以 **跑馬燈（Marquee）** 的形式自動捲動顯示「**歷史上的今天**」重大事件。使用者無需打開 APP，即可在主畫面直接瀏覽當日歷史大事。

---

## 二、功能需求

### 2.1 核心功能

| 編號 | 功能說明 | 優先級 |
|------|----------|--------|
| F-01 | Widget 以跑馬燈形式捲動顯示當日歷史事件 | 必要 |
| F-02 | 每天自動更新當日事件資料 | 必要 |
| F-03 | 支援 Android 主畫面 Widget | 必要 |
| F-04 | 支援 iOS 主畫面 Widget（WidgetKit） | 選擇性 |
| F-05 | 可設定跑馬燈捲動速度 | 建議 |
| F-06 | 點擊 Widget 可開啟 APP 查看詳細內容 | 建議 |
| F-07 | 可設定事件語言（中文／英文） | 建議 |
| F-08 | 可設定事件分類（歷史、科學、政治等） | 選擇性 |

### 2.2 Widget 顯示規格

- **尺寸**：支援 4×1 或 4×2 格（Android）、medium/large（iOS）
- **更新頻率**：每日零時自動刷新（或每 3 小時更新一次）
- **顯示格式**：`【西元年份】事件標題` 依序捲動
- **跑馬燈方向**：由右向左（或由下向上，可設定）

### 2.3 非功能需求

- 離線可用：首次啟動後需預載當月資料，確保無網路時仍可顯示
- 低電耗：背景更新需使用低功耗方式（JobScheduler / WorkManager）
- 隱私：不蒐集任何個人資料
- 多語言：UI 支援繁體中文、英文

---

## 三、資料來源比較

| 方案 | 來源 | 語言支援 | 費用 | 穩定性 | 授權 |
|------|------|----------|------|--------|------|
| **Wikipedia On This Day API** | [Wikimedia REST API](https://api.wikimedia.org/wiki/Feed_API/reference/on_this_day) | 多語言（含中文） | 免費 | 高 | CC BY-SA |
| **歷史上的今天（中文 Wikipedia 子集）** | `https://zh.wikipedia.org/api/rest_v1/feed/onthisday/events/{MM}/{DD}` | 中文 | 免費 | 高 | CC BY-SA |
| **本機靜態資料庫（SQLite / JSON）** | 預先整理好的 JSON 或 SQLite DB，打包進 APP | 任意 | 免費 | 極高 | 視資料來源 |
| **History.com API（非官方爬蟲）** | 爬取公開網頁 | 英文 | 免費（有風險） | 低 | 有法律風險 |
| **自建後端 API** | 自行整理資料並部署 API Server | 任意 | 視規模 | 高 | 自定 |

> **建議**：優先使用 **Wikimedia REST API**（免費、穩定、支援中文、無需 API Key），輔以本機 SQLite 快取確保離線可用。

---

## 四、技術方案比較

---

### 方案一：Flutter（跨平台，iOS + Android）

#### 架構概覽

```
Flutter APP
├── 主畫面（歷史大事列表）
├── Widget 層（透過 home_widget 套件橋接原生 Widget）
│   ├── Android：AppWidgetProvider（Kotlin）
│   └── iOS：WidgetKit Extension（Swift）
├── 資料層：http + dio（API 呼叫）+ sqflite（本機快取）
└── 背景排程：workmanager（背景定時更新）
```

#### 主要開源套件

| 套件 | 用途 | pub.dev 連結 |
|------|------|-------------|
| [`home_widget`](https://pub.dev/packages/home_widget) | Flutter 與原生 Widget 雙向通訊 | ★★★★☆ |
| [`workmanager`](https://pub.dev/packages/workmanager) | 背景定時工作（iOS / Android） | ★★★★☆ |
| [`dio`](https://pub.dev/packages/dio) | HTTP Client，呼叫 Wikipedia API | ★★★★★ |
| [`sqflite`](https://pub.dev/packages/sqflite) | 本機 SQLite 快取 | ★★★★★ |
| [`marquee`](https://pub.dev/packages/marquee) | 跑馬燈文字捲動元件（APP 內部使用） | ★★★★☆ |
| [`flutter_riverpod`](https://pub.dev/packages/flutter_riverpod) | 狀態管理 | ★★★★★ |
| [`intl`](https://pub.dev/packages/intl) | 日期格式化、多語言 | ★★★★★ |

#### Widget 跑馬燈限制說明

> ⚠️ **重要限制**：Android AppWidget 與 iOS WidgetKit 均**不支援真正的動畫**（包含跑馬燈）。  
> 解決方案：
> 1. **假跑馬燈**：Widget 每 30 分鐘自動刷新，每次顯示不同的一則事件（模擬輪播效果）
> 2. **Android 端**：使用 `RemoteViews` + `TextSwitcher` 加上進入／離開動畫實現輕微動態
> 3. **Android 端（進階）**：使用 `Glance` API（Jetpack Compose for Widgets），支援更豐富的 UI

#### 優缺點

| 優點 | 缺點 |
|------|------|
| 單一程式碼庫，同時支援 iOS 和 Android | Widget 部分仍需撰寫原生程式碼（Kotlin / Swift） |
| Flutter 生態完善，套件豐富 | `home_widget` 橋接層學習曲線 |
| UI 一致性高 | Flutter APP 體積較大（~20MB） |
| 社群活躍，文件完整 | 真正的跑馬燈動畫在 Widget 中無法實現 |

---

### 方案二：Android 原生（Kotlin + AppWidget / Jetpack Glance）

#### 架構概覽

```
Android APP（Kotlin）
├── 主 Activity（歷史大事列表）
├── AppWidget（AppWidgetProvider 或 Jetpack Glance）
│   ├── RemoteViews / GlanceAppWidget
│   └── 自訂跑馬燈 View（TextSwitcher + 動畫）
├── 資料層：Retrofit2 + Room Database
└── 背景排程：WorkManager
```

#### 主要開源函式庫

| 函式庫 | 用途 | 來源 |
|--------|------|------|
| [Retrofit2](https://square.github.io/retrofit/) | HTTP Client | Square（開源） |
| [Room](https://developer.android.com/training/data-storage/room) | 本機 SQLite ORM | Google Jetpack |
| [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | 背景排程任務 | Google Jetpack |
| [Jetpack Glance](https://developer.android.com/jetpack/compose/glance) | Compose 風格 Widget UI | Google Jetpack |
| [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) | 非同步處理 | JetBrains |
| [Gson / Moshi](https://github.com/google/gson) | JSON 解析 | Google / Square |
| [Marquee（自訂 View）](https://github.com/traex/MarqueeView) | 跑馬燈元件 | 開源社群 |

#### Widget 跑馬燈實現方式（Android 原生）

```kotlin
// 使用 TextSwitcher + AlphaAnimation 模擬輪播
// 在 AppWidgetProvider 中搭配 RemoteViews
val views = RemoteViews(context.packageName, R.layout.widget_layout)
views.setTextViewText(R.id.tv_event, currentEvent)
// 每 30 分鐘由 WorkManager 更新一則事件
```

> **進階**：使用 [Jetpack Glance](https://developer.android.com/jetpack/compose/glance) + `LazyColumn` 搭配自動刷新，可達到類似跑馬燈的視覺效果。

#### 優缺點

| 優點 | 缺點 |
|------|------|
| 完整原生 Android API 控制 | 僅支援 Android |
| AppWidget 框架最成熟 | 需熟悉 AppWidget 生命週期（較複雜） |
| Glance API 支援現代 Compose UI | Glance 仍在快速演進中 |
| 效能最佳，啟動速度快 | 需要額外維護 iOS 版本（若有需求） |

---

### 方案三：iOS 原生（Swift + WidgetKit）

#### 架構概覽

```
iOS APP（Swift / SwiftUI）
├── 主畫面（HistoryListView）
├── Widget Extension（WidgetKit）
│   ├── TimelineProvider（提供每日時間軸資料）
│   └── WidgetView（SwiftUI 視圖）
├── 資料層：URLSession + CoreData / UserDefaults（App Group 共享）
└── 背景更新：Background App Refresh + WidgetKit Timeline
```

#### 主要開源框架

| 框架 / 函式庫 | 用途 | 來源 |
|--------------|------|------|
| [WidgetKit](https://developer.apple.com/documentation/widgetkit) | iOS Widget 官方框架 | Apple |
| [SwiftUI](https://developer.apple.com/xcode/swiftui/) | Widget 與 APP UI | Apple |
| [Alamofire](https://github.com/Alamofire/Alamofire) | HTTP 網路請求 | 開源 |
| [SwiftyJSON](https://github.com/SwiftyJSON/SwiftyJSON) | JSON 解析 | 開源 |
| [CoreData](https://developer.apple.com/documentation/coredata) | 本機資料持久化 | Apple |
| [MarqueeLabel](https://github.com/cbpowell/MarqueeLabel) | UIKit 跑馬燈標籤 | 開源（UIKit only） |

#### Widget 跑馬燈限制說明（iOS）

> ⚠️ **WidgetKit 嚴格限制**：iOS Widget **不支援任何動畫**（包含跑馬燈、捲動）。  
> 解決方案：  
> 1. **Timeline 輪播**：使用 `TimelineEntry` 定義每 15 分鐘切換一則事件的時間軸
> 2. **視覺模擬**：用靜態文字截斷 + "..." 顯示，點擊後開啟 APP 看完整內容
> 3. **Live Activities（iOS 16.2+）**：Dynamic Island / 鎖定畫面動態島可實現更豐富的即時更新

```swift
struct HistoryTimelineProvider: TimelineProvider {
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
        let events = fetchTodayEvents()
        var entries: [HistoryEntry] = []
        for (index, event) in events.enumerated() {
            let date = Calendar.current.date(byAdding: .minute, value: index * 15, to: Date())!
            entries.append(HistoryEntry(date: date, event: event))
        }
        let timeline = Timeline(entries: entries, policy: .after(entries.last!.date))
        completion(timeline)
    }
}
```

#### 優缺點

| 優點 | 缺點 |
|------|------|
| WidgetKit 是 iOS Widget 最標準做法 | 僅支援 iOS |
| Timeline 機制可精確控制事件輪播 | WidgetKit 限制嚴格（無動畫、無網路請求） |
| SwiftUI 語法直觀，UI 精美 | 需 Xcode，僅限 macOS 開發 |
| Live Activities 提供更多動態可能性 | 上架需 Apple Developer 帳號（$99/年） |

---

### 方案四：React Native（跨平台）

#### 架構概覽

```
React Native APP
├── 主畫面（React Native）
├── Widget 層
│   ├── Android：react-native-android-widget
│   └── iOS：react-native-widget-extension（實驗性）
├── 資料層：axios + AsyncStorage / SQLite
└── 背景排程：react-native-background-fetch
```

#### 主要開源套件

| 套件 | 用途 | npm 連結 |
|------|------|---------|
| [react-native-android-widget](https://www.npmjs.com/package/react-native-android-widget) | Android Widget 支援 | ★★★☆☆（成熟度普通） |
| [react-native-widget-extension](https://github.com/boltcode-js/react-native-widget-extension) | iOS WidgetKit 橋接 | ★★☆☆☆（實驗性） |
| [react-native-marquee](https://www.npmjs.com/package/react-native-marquee) | APP 內部跑馬燈元件 | ★★★☆☆ |
| [axios](https://axios-http.com/) | HTTP Client | ★★★★★ |
| [react-native-sqlite-storage](https://www.npmjs.com/package/react-native-sqlite-storage) | 本機 SQLite | ★★★★☆ |
| [react-native-background-fetch](https://www.npmjs.com/package/react-native-background-fetch) | 背景排程 | ★★★★☆ |
| [Zustand](https://github.com/pmndrs/zustand) | 輕量狀態管理 | ★★★★★ |

#### 優缺點

| 優點 | 缺點 |
|------|------|
| JavaScript / TypeScript，前端工程師易上手 | Widget 套件成熟度低，問題較多 |
| 跨平台一份程式碼 | iOS Widget 支援為實驗性，風險高 |
| 生態系豐富 | 效能略遜於原生 |
| Hot Reload 開發效率高 | React Native 版本升級可能破壞 Widget 橋接 |

---

## 五、方案總比較表

| 評估項目 | 方案一：Flutter | 方案二：Android 原生 | 方案三：iOS 原生 | 方案四：React Native |
|----------|:--------------:|:------------------:|:--------------:|:------------------:|
| **跨平台支援** | iOS + Android | Android 僅 | iOS 僅 | iOS + Android |
| **Widget 成熟度** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **跑馬燈可行性** | ⭐⭐⭐（輪播） | ⭐⭐⭐⭐（輪播） | ⭐⭐⭐（Timeline） | ⭐⭐（不穩定） |
| **開發難度** | 中 | 中高 | 中高 | 低中 |
| **維護成本** | 低（單一語言） | 中（Kotlin） | 中（Swift） | 低中（JS/TS） |
| **社群資源** | 豐富 | 豐富 | 豐富 | 豐富 |
| **開源工具完整度** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **推薦指數** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐（Android 優先） | ⭐⭐⭐⭐（iOS 優先） | ⭐⭐ |

---

## 六、推薦方案

### 🥇 首選：方案二（Android 原生 Kotlin）+ 方案三（iOS 原生 Swift）分開開發

**適用情境**：若平台明確，分開做品質最高。

- **Android**：Kotlin + Jetpack Glance + WorkManager + Retrofit2 + Room
- **iOS**：Swift + SwiftUI + WidgetKit + Alamofire + CoreData

---

### 🥈 次選：方案一（Flutter 跨平台）

**適用情境**：希望一套程式碼同時支援 iOS 和 Android，且開發資源有限。

- **Flutter** + `home_widget` + `workmanager` + `dio` + `sqflite`
- Widget 部分仍需少量 Kotlin/Swift 原生程式碼，但主體邏輯共用

---

### ❌ 不建議：方案四（React Native）

Widget 套件成熟度不足，尤其 iOS 端風險過高，不適合 Widget 為核心功能的 APP。

---

## 七、資料流設計（通用）

```
[使用者手機] 每日 00:05（本地時間）
    │
    ├─ WorkManager / Background Fetch 觸發
    │
    ├─ 呼叫 Wikipedia On This Day API
    │   GET https://zh.wikipedia.org/api/rest_v1/feed/onthisday/events/{MM}/{DD}
    │
    ├─ 解析 JSON，取出事件列表
    │
    ├─ 存入本機資料庫（SQLite / Room / CoreData）
    │
    └─ 通知 Widget 更新 → Widget 從本機 DB 讀取資料 → 顯示跑馬燈
```

### Wikipedia API 回應格式範例

```json
{
  "events": [
    {
      "year": 1969,
      "text": "阿波羅11號太空人阿姆斯壯成為首位登陸月球的人類",
      "pages": [
        {
          "title": "阿波羅11號",
          "extract": "..."
        }
      ]
    }
  ]
}
```

---

## 八、專案里程碑（供參考）

| 階段 | 工作項目 | 預估工時 |
|------|----------|----------|
| M0 | 確認方案、確定平台目標 | 1 天 |
| M1 | 建立專案骨架、整合 Wikipedia API | 3 天 |
| M2 | 實作主 APP 歷史事件列表頁 | 3 天 |
| M3 | 實作 Widget 輪播顯示 | 5 天 |
| M4 | 背景排程自動更新 | 2 天 |
| M5 | 離線快取（SQLite） | 2 天 |
| M6 | UI 美化、設定頁（語言、速度） | 3 天 |
| M7 | 測試、除錯、效能優化 | 5 天 |
| M8 | 上架（Google Play / App Store） | 3 天 |
| **合計** | | **~27 天** |

---

## 九、開放原始碼參考專案

以下為現有類似開源專案，可作為開發參考或直接使用：

| 專案 | 平台 | GitHub |
|------|------|--------|
| [On This Day (Flutter)](https://github.com/flutter/samples) | Flutter | 參考 Flutter 官方範例 |
| [WikipediaKit](https://github.com/felixhandte/WikipediaKit) | iOS Swift | Wikipedia API Swift 封裝 |
| [wikipedia-android](https://github.com/wikimedia/apps-android-wikipedia) | Android | Wikipedia 官方 Android APP（Kotlin） |
| [HistoryWidget（社群）](https://github.com/search?q=on+this+day+widget+android) | Android | 搜尋關鍵字參考 |

---

## 十、附錄：Widget 跑馬燈技術限制總結

由於 Android AppWidget 和 iOS WidgetKit 均不支援真正的動畫，本專案的「跑馬燈」效果需採用以下替代方案：

| 平台 | 替代方案 | 說明 |
|------|----------|------|
| Android | 定時輪播（WorkManager 每 15-30 分鐘更新） | Widget 每次顯示不同的一則事件 |
| Android（進階） | Glance + 動態圖片（Canvas 繪製滾動字幕） | 使用 `RemoteViews.setImageViewBitmap` 顯示動態繪製的跑馬燈圖片 |
| iOS | WidgetKit Timeline 輪播 | TimelineProvider 返回多個 Entry，每15分鐘切換一則 |
| iOS（進階） | Live Activities | 鎖定畫面動態更新（iOS 16.2+，需 ActivityKit） |

---

*本需求書僅供方案評估使用，正式開發前請依選定方案進一步細化技術規格。*
