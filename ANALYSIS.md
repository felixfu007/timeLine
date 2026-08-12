# 技術選型分析文件：歷史上的今天跑馬燈 Widget APP

> 版本：v1.0
> 日期：2026-07-27
> 狀態：分析完成，待確認後進入開發（對應 `REQUIREMENTS.md` v1.0）
> 撰寫角色：系統分析師

---

## 〇、文件目的

本文件承接 `REQUIREMENTS.md` 的方案評估結果，針對「開發資源有限（單人／小團隊）」的實際情境，重新覆核技術選型，並將需求書中偏概念性的內容（F-01～F-08、非功能需求、里程碑）轉化為可直接交付給開發與 QA 的具體規格。凡本文件與 `REQUIREMENTS.md` 有出入之處，以本文件為最新決策依據，並建議日後同步回寫需求書。

---

## 一、選型決策

### 結論：第一階段先做 **Android 原生（Kotlin + Jetpack Glance + WorkManager + Retrofit2 + Room）**，iOS 原生延後至第二階段。

需求書第六章建議「Android 原生 + iOS 原生分開開發」為首選、Flutter 為次選。本文件**同意「原生優先」的技術判斷**，但進一步收斂為「先單一平台、再擴展」，理由如下：

1. **需求書的首選方案隱含雙平台同時投入的人力假設**。若專案實際上是單人或極小團隊（本專案目前狀態即為個人 GitHub 專案、尚未組建團隊），同時維護兩套原生程式碼庫（Kotlin + Swift、兩套 CI、兩套上架流程）會拉長首次上線時間，且在需求／資料流尚未經市場驗證前過度投入。
2. **Widget 才是本專案的核心價值，而 Widget 邏輯本身無法跨平台共用**。不論選 Flutter、React Native 或雙原生，Widget 層（AppWidget/Glance vs WidgetKit）都必須各自用原生語言實作。這意味著 Flutter／RN 主打的「單一程式碼庫」優勢，在本專案中只覆蓋「App 內列表頁」這種非核心功能，卻仍要多背一層橋接框架（`home_widget`、`workmanager` 等）的學習成本與風險。跨平台框架的投資回報率在此專案被稀釋。
3. **先驗證再擴張，降低沉沒成本風險**。Widget 輪播的可行性、Wikipedia API 資料完整度、電量與更新頻率的實際體感，都需要在真實裝置上驗證。用單一平台（Android）先跑通「資料流 → 本機快取 → Widget 輪播 → 使用者反饋」全鏈路，之後再決定是否／如何投入 iOS，風險最小。
4. **選 Android 而非 iOS 作為起點的具體理由**：
   - Widget 框架成熟度兩者相近（皆 ⭐⭐⭐⭐⭐），但 Android 開發與測試門檻更低：不需要 macOS/Xcode，可用免費模擬器或任意 Android 實機測試。
   - 上架成本更低：Google Play 開發者帳號一次性 $25 美元；Apple Developer Program 為 $99 美元／年，且審核流程較長，適合等產品成型後再投入。
   - WorkManager／Glance 的「定時輪播模擬跑馬燈」實作路徑相對直觀，且 Android 對背景排程的最小週期限制（15 分鐘）與需求書 F-05／附錄的輪播間隔設計（15–30 分鐘）天然吻合。

### 後續擴展 iOS 的路徑

- **不是直接重寫，而是「架構複用＋程式碼重寫」**：Kotlin 與 Swift 程式碼無法共用，但以下決策資產可以直接搬到 iOS 專案：Wikipedia API 契約與資料模型（`year` / `text` / `pages`）、本機快取 schema 的欄位設計、輪播間隔與資料保鮮策略、多語言字串 key、非功能需求量測標準（本文件第五章）。
- **觸發擴展的判斷點（建議，非強制）**：Android 版本上架並取得穩定運行、崩潰率與電量投訴在可接受範圍、且確認有 iOS 使用者需求後，才啟動 iOS 原生開發（Swift + SwiftUI + WidgetKit + Alamofire + CoreData，沿用需求書方案三的模組設計）。
- **若日後人力仍然有限但雙平台需求已確認**：可重新評估是否放棄「雙原生分開維護」，改採 Flutter 重寫以降低長期維護成本；但這是「驗證後的二次決策」，不應在專案起點就假設。
- **若專案實際上已有多名工程師可平行開發雙平台**：則本文件的「先單一平台」前提不成立，應回到需求書第六章的首選（Android 原生 + iOS 原生同時分開進行）。

---

## 二、技術選型理由（整合需求書第三、四、五、六章）

| 考量維度 | 分析 |
|---|---|
| 開發資源有限 | 單人/小團隊下，同時維護兩套原生代碼庫或一套跨平台橋接框架，都會拉高首次交付的複雜度。先做單平台可最快產出可用產品、驗證核心假設。 |
| Widget 成熟度 | Android AppWidget/Glance 與 iOS WidgetKit 皆為 ⭐⭐⭐⭐⭐（需求書表五），遠高於 Flutter 的橋接套件 `home_widget`（⭐⭐⭐～⭐⭐⭐☆，本文件下修，因其仍需原生程式碼且社群回報偶有版本相容性問題）與 React Native（⭐⭐，iOS 端為實驗性套件）。這代表無論最終長期策略如何，Widget 層都應優先信任原生框架而非橋接層。 |
| 跑馬燈可行性 | 兩大平台皆**不支援真正動畫**，只能靠「輪播（Timeline / 定時刷新）」模擬。這個限制與框架選擇無關（Flutter/RN 底層一樣要透過原生 Widget API），因此跨平台框架在此無任何加分，反而多一層橋接風險。 |
| 開發難度／維護成本 | 需求書標示 Android 原生為「開發難度中高、維護成本中」，看似高於 RN（低中），但 RN 的「低」建立在 Widget 套件成熟度低、iOS 端實驗性的前提上，屬於「隱藏成本延後爆發」的風險，不建議為 Widget 為核心功能的產品採用。 |
| 開源工具完整度 | Android 生態（Retrofit2、Room、WorkManager、Glance）皆為 Google Jetpack 官方或 Square 出品，長期維護有保障，優於 RN 生態中維護活躍度不明的第三方 Widget 套件。 |

**結論**：需求書的資料已足以支持「原生優先」，本文件在此基礎上加入「資源有限 → 先單平台」的實務判斷，兩者並不矛盾——先做最成熟、最低門檻的單一原生平台（Android），把「跨平台一致性」問題延後到需求明確後再決定。

---

## 三、細化模組拆解（Android 原生，第一階段）

```
Android APP（Kotlin + Jetpack Compose）
├── app 模組（主程式）
│   ├── UI 層：MainActivity + Compose（歷史事件列表、事件詳情、設定頁）
│   ├── di：Hilt（依賴注入）
│   └── ViewModel + Kotlin Coroutines / Flow
│
├── data 模組（資料層）
│   ├── remote：Retrofit2 + Moshi
│   │   └── WikipediaOnThisDayApi（GET /feed/onthisday/events/{MM}/{DD}，zh 與 en 兩個 baseUrl）
│   ├── local：Room Database
│   └── repository：HistoryEventRepository（先讀快取，快取過期或缺當日資料才發網路請求）
│
├── widget 模組
│   ├── GlanceAppWidget + GlanceAppWidgetReceiver（主要實作）
│   ├── fallback：AppWidgetProvider + RemoteViews（若 Glance 版本相容性出問題時的備援）
│   └── WidgetRotationState（記錄目前輪播到第幾則事件、上次更新時間）
│
└── scheduler 模組
    ├── WorkManager：DailyFetchWorker（PeriodicWorkRequest，每日 00:05 觸發，含 NetworkRequired constraint）
    └── WorkManager：WidgetRotationWorker（PeriodicWorkRequest，最小週期 15 分鐘，僅讀本機 DB、不發網路請求）
```

### 主要套件對應

| 模組 | 套件/框架 | 用途 |
|---|---|---|
| API Client | Retrofit2 + Moshi | 呼叫 Wikimedia REST API，解析 JSON |
| 本機資料庫 | Room | 事件快取、設定持久化 |
| Widget UI | Jetpack Glance | 現代 Compose 風格 Widget，支援 4×1/4×2 |
| 背景排程 | WorkManager | 每日抓取、輪播刷新兩類週期任務 |
| 非同步 | Kotlin Coroutines / Flow | Repository 與 ViewModel 間的資料流 |
| DI | Hilt | 模組解耦、方便測試 |
| 設定持久化 | Jetpack DataStore Preferences | 語言、輪播間隔、分類篩選等使用者設定 |

### Room Schema（草案）

```
表：history_event
- id                 INTEGER PRIMARY KEY AUTOINCREMENT
- month              INTEGER      -- 1~12
- day                INTEGER      -- 1~31
- year               INTEGER      -- 事件發生西元年
- text_zh            TEXT         -- 中文事件描述
- text_en            TEXT NULL    -- 英文事件描述（延後填充，見 F-07 驗收條件）
- category_tag       TEXT NULL    -- 分類（見 F-08，Phase 2+，來源 API 無原生分類，需自行推斷）
- source_updated_at  INTEGER      -- 從 API 取得的時間戳
- cached_at          INTEGER      -- 寫入本機的時間戳，用於過期判斷與清理

表：widget_state
- widget_id          INTEGER PRIMARY KEY   -- Android appWidgetId
- current_index      INTEGER      -- 目前輪播到第幾則
- last_rotated_at     INTEGER

表：app_settings（或改用 DataStore，二者擇一，不重複儲存）
- language           TEXT         -- "zh-Hant" / "en"
- rotation_interval_min INTEGER   -- 15 / 30 / 60
- category_filter    TEXT NULL
- scroll_direction    TEXT         -- 保留欄位，對應需求書 2.2 節方向設定，但 Widget 端僅影響輪播/排版，非真捲動
```

---

## 四、功能驗收條件（F-01～F-08）

| 編號 | 優先級 | 驗收條件（Acceptance Criteria） |
|---|---|---|
| F-01 | 必要 | 1) Widget 顯示格式為「【西元年份】事件標題」；2) 事件內容以**定時輪播**（非連續文字捲動）方式切換，每次刷新只顯示一則事件；3) 事件標題超過 Widget 可視寬度時以「…」截斷；4) 首次安裝且尚無快取資料時，Widget 顯示明確的「載入中」或「尚無資料」提示，不得顯示空白或崩潰。 |
| F-02 | 必要 | 1) 每日本地時間 00:05 觸發一次資料更新（WorkManager PeriodicWorkRequest）；2) 更新失敗時（無網路/API 錯誤）保留前一次成功快取，不清空既有資料；3) 更新成功後 24 小時內不重複發送當日相同請求（避免重試造成的重複流量）；4) 若使用者手動切換日期跨零時（如時區變更），下一次排程週期內須以裝置當地日期為準重新拉取。 |
| F-03 | 必要 | 1) 支援 4×1 與 4×2 兩種 Widget 尺寸的獨立版面（4×1 僅顯示標題，4×2 可加副標/年代軸）；2) Widget 可從主畫面「新增小工具」選單正常加入與移除；3) 多個同款 Widget 實例（例如使用者加兩個）互不干擾，各自維護輪播進度（對應 `widget_state` 表以 widgetId 為 key）。 |
| F-04 | 選擇性（Phase 1 不做） | 第一階段不實作。驗收僅要求：資料層／API 契約與快取 schema 設計不得綁死 Android 專屬型別，確保 Phase 2 導入 iOS WidgetKit 時可直接複用資料模型與 API 呼叫邏輯（不要求程式碼共用，只要求「決策資產」可複用）。 |
| F-05 | 建議（需求重新定義，見第六章風險說明） | 1) 設定頁提供「輪播間隔」選項：15 分鐘 / 30 分鐘 / 60 分鐘（不提供「捲動速度」，因平台限制無法實現連續捲動動畫，需求文字建議由「跑馬燈捲動速度」改為「輪播間隔」）；2) 使用者變更設定後，最晚於下一個 WorkManager 排程週期內生效；3) 因 Android 系統 Doze/電量最佳化可能延遲執行，允許實際生效時間與設定值有 ±15 分鐘誤差，此為已知限制而非缺陷。 |
| F-06 | 建議 | 1) 點擊 Widget 目前顯示中的事件，開啟 App 並導向該事件的詳情頁（而非固定開啟首頁）；2) 若該事件有關聯的 Wikipedia `pages`，詳情頁顯示標題與摘要（`extract`），並附來源連結。 |
| F-07 | 建議 | 1) 設定頁可切換「繁體中文 / English」；2) 切換後 App 內 UI 字串（含日期格式）立即套用，Widget 文字於下一次刷新週期套用；3) 若 API 回應缺少對應語言欄位（例如英文版 On This Day 內容與中文版事件集合不完全一致），需有明確的 fallback 規則（優先顯示可取得語言，並在 UI 上不留空白列）。 |
| F-08 | 選擇性（風險較高，建議下修至 Phase 3，見第六章） | 1) 設定頁提供分類篩選（歷史/科學/政治…）選項的 UI 先行保留；2) 因 Wikimedia On This Day API 本身不提供分類欄位，本階段驗收僅要求「UI 骨架存在、預設不啟用篩選」，實際分類邏輯（規則式關鍵字比對或人工標記資料集）列為待確認事項，不納入 Phase 1 驗收範圍。 |

---

## 五、非功能需求量測標準（對應需求書 2.3 節）

| 需求 | 具體可驗證標準 |
|---|---|
| 離線可用 | 1) 首次啟動且網路可用時，一次性預載「當月全部天數」的歷史事件資料寫入 Room（例如 7 月即預載 7/1～7/31 共 31 天的事件集合）；2) 本機快取採滾動視窗保留至少 35 天資料（涵蓋當月＋前後緩衝），避免月初/月末因裝置時鐘或時區問題找不到資料；3) 在飛航模式（完全無網路）下，Widget 與 App 皆能正常顯示「最近一次成功快取」的當日事件，不得顯示空白、錯誤訊息或崩潰；4) 首次安裝且從未成功連線過的情況（無任何快取）視為已知的「首次使用受限」情境，需顯示明確提示文案，不視為離線可用失敗。 |
| 低電耗 | 1) 每日資料抓取使用 WorkManager `PeriodicWorkRequest`，最小週期不低於 15 分鐘（Android 系統下限），不使用 `AlarmManager` 精確喚醒或 WakeLock 常駐；2) 單次背景任務（含網路請求）執行時間目標 < 5 秒，網路請求 timeout 設為 10 秒並設重試上限（例如 3 次、指數退避）；3) Widget 輪播刷新僅讀本機 Room 資料，不觸發網路請求；4) 需求書所述「每 3 小時更新一次」與「WorkManager」的組合視為本標準已涵蓋，不另設更頻繁的網路輪詢。 |
| 隱私 | 1) App 不整合任何第三方分析（Analytics）、廣告 SDK、使用者行為追蹤；2) 不要求註冊/登入，不上傳任何裝置識別碼或使用資料至自建或第三方伺服器；3) AndroidManifest 僅宣告 `INTERNET` 與（若採用開機後重建排程）`RECEIVE_BOOT_COMPLETED` 權限，不申請通訊錄、定位、儲存等非必要權限；4) 所有使用者設定與快取資料僅存於裝置本機（Room/DataStore），解除安裝即完整清除。 |
| 多語言 | 1) UI 文字全部以 Android string resources 管理，至少提供 `values`（繁體中文為預設）與 `values-en`（英文）兩套資源；2) 日期／年份呈現依 `Locale` 自動調整格式（例如「1969 年」vs「1969」）；3) 語言切換為 App 內設定項（見 F-07），不僅依賴系統語言，允許使用者獨立於系統語言之外指定 App 顯示語言。 |

---

## 六、風險與待決事項

### 6.1 核心矛盾：F-05「可設定跑馬燈捲動速度」與 Widget 平台限制

**問題本質**：Android AppWidget/Glance 與 iOS WidgetKit 皆不支援自訂的連續動畫（包含文字水平捲動），且 Android 對 Widget 更新頻率有系統級節流（建議最小間隔 30 分鐘，`WorkManager` 週期性任務下限為 15 分鐘）。因此 F-05 字面上要求的「捲動速度」在技術上無法實現——使用者無法感受到「文字滑動快慢」的差異，只能感受到「多久換一則新事件」的差異。

**調和建議**：

1. **需求重新定義（優先採用）**：將 F-05 的使用者文案與設定項由「跑馬燈捲動速度」改為「輪播間隔」（15 / 30 / 60 分鐘），並在 App 內以文案說明（例如設定頁註記「Widget 因系統限制以輪播方式呈現，非連續捲動」），管理使用者預期，避免上架後被誤解為 Bug。
2. **App 內提供真跑馬燈作為補償體驗**：App 主畫面（非 Widget）不受 Widget 平台限制，可用一般 View/Compose 動畫或 `TextSwitcher` 實現真正的連續捲動效果，滿足使用者對「跑馬燈」視覺期待，同時明確區分「App 內＝真跑馬燈」「Widget＝輪播摘要」兩種體驗，需求書與 UI 文案都應清楚切割。
3. **Widget 端的輕量動態感（可選、非必要）**：使用 `TextSwitcher` 搭配進場/退場的淡入淡出或位移動畫，在**每次輪播切換的瞬間**提供短暫過場效果，模擬「有東西正在滑動」的錯覺；但不建議採用「Canvas 繪製捲動字幕轉 Bitmap 再高頻更新」的方案，因為這會觸發 Android 對 Widget 更新頻率的系統節流與額外耗電，與「低電耗」非功能需求衝突，得不償失。
4. **後續（iOS）**：對應 WidgetKit 的 `TimelineProvider`，以多個 `TimelineEntry` 實現「每 N 分鐘換一則」，概念與 Android 輪播一致，可延用同一套「輪播間隔」設定值。

### 6.2 其他風險與待決事項

| 風險/待決事項 | 說明 | 建議 |
|---|---|---|
| Wikipedia API 資料完整度未驗證 | 部分冷門月/日的事件筆數可能偏少，或中英文版事件集合不完全對應（F-07 fallback 邏輯依賴此假設） | 開發前實際呼叫 API 抽樣（例如 2 月 29 日、國定假日前後）確認資料量與欄位穩定性，納入 M1 工作項 |
| Wikimedia API 使用規範 | 官方要求呼叫端帶合規 `User-Agent`，否則可能被限流或封鎖；目前為免費、無 SLA 服務 | 在 Retrofit 攔截器中加入符合 Wikimedia API 政策的 User-Agent，並實作重試/退避與「請求失敗回退舊快取」機制（已納入 F-02、離線可用標準） |
| Android Widget 更新延遲 | Doze 模式、電量最佳化、廠商客製系統（如小米/OPPO 的背景限制）可能導致 WorkManager 實際觸發時間與設定值有偏差 | 在驗收標準中明確允許 ±15 分鐘誤差（已寫入 F-05），並在使用者文件中說明，避免被誤判為 Bug |
| F-08 分類功能可行性存疑 | Wikimedia On This Day API 不提供分類欄位，需自建規則或另找資料源，工作量可能被需求書低估 | 建議明確下修為 Phase 3（可選），Phase 1／2 只做 UI 骨架，不承諾實際分類效果 |
| CC BY-SA 授權合規 | Wikipedia 資料採 CC BY-SA 授權，需在 App 內做出處標註 | 在「關於」頁與事件詳情頁加入來源與授權聲明，必要時徵詢法務意見 |
| 單平台起步的組織風險 | 若專案實際上已有多人力可平行開發，本文件「先做 Android」的判斷前提就不成立 | 開發啟動前（M0）需明確確認實際可投入人力，若人力充足應改採需求書原始首選（雙原生平行開發） |
| iOS 擴展時的框架再評估 | 若日後雙平台需求明確且維護兩套原生成本過高 | 屆時重新評估是否改採 Flutter 統一開發，但不應在專案起點預先假設 |

---

## 七、里程碑覆核（對照需求書第八章 M0～M8）

需求書第八章的 ~27 天估算，未明確標註是否已涵蓋雙平台工作量（例如 M8「上架 Google Play / App Store」字面上同時列出兩個商店，但 M3「Widget 輪播」只估 5 天，若要同時涵蓋 Android Glance 與 iOS WidgetKit 兩套輪播邏輯，5 天明顯偏緊）。**判斷：原估算較適合套用在單一平台身上，若字面上要求同時完成雙平台，則明顯低估。**

### 覆核結果：以「Phase 1 僅做 Android」重新估算

| 階段 | 工作項目 | 原估工時 | 重新估算（Android-only） | 差異說明 |
|---|---|---|---|---|
| M0 | 確認方案、確定平台目標 | 1 天 | 1 天（已由本文件完成分析，僅需拍板確認） | 持平 |
| M1 | 建立專案骨架、整合 Wikipedia API | 3 天 | 3 天 | 持平，含 Retrofit2 + Room + API 抽樣驗證 |
| M2 | 主 APP 歷史事件列表頁 | 3 天 | 2.5 天 | Compose 單平台實作略省 |
| M3 | Widget 輪播顯示 | 5 天 | 4 天 | 原估可能隱含雙平台份量，單平台 Glance + WorkManager 輪播邏輯略低於原估 |
| M4 | 背景排程自動更新 | 2 天 | 2 天 | 持平 |
| M5 | 離線快取（SQLite/Room） | 2 天 | 1.5 天 | 與 M1 部分工作重疊，實際新增工作量略低 |
| M6 | UI 美化、設定頁（語言、輪播間隔） | 3 天 | 2.5 天 | F-05 需求已簡化為「輪播間隔」選項，UI 複雜度降低 |
| M7 | 測試、除錯、效能優化 | 5 天 | 4 天 | 含電量/Doze 情境測試，單平台聚焦測試範圍 |
| M8 | 上架 Google Play | 3 天 | 1.5 天 | 僅一個商店、免 Apple 審核等待，工時明顯低於原估的雙商店假設 |
| **合計** | | **~27 天** | **~22.5 天（約 4.5 週）** | 單平台估算略低於原總估 |

### Phase 2（iOS 擴展）概估（供未來排程參考，非本階段承諾）

| 階段 | 工作項目 | 估算工時 |
|---|---|---|
| iOS-M1 | WidgetKit 專案骨架、URLSession + CoreData | 3 天 |
| iOS-M2 | 主畫面（SwiftUI） | 2 天 |
| iOS-M3 | WidgetKit TimelineProvider 輪播 | 4 天 |
| iOS-M4 | Background Refresh 排程 | 2 天 |
| iOS-M5 | 離線快取（CoreData / App Group 共享） | 1.5 天 |
| iOS-M6 | UI 美化、設定頁 | 2 天 |
| iOS-M7 | 測試、除錯 | 3 天 |
| iOS-M8 | App Store 上架（含開發者帳號準備） | 2 天 |
| **合計** | | **~19.5 天（約 4 週）** |

**結論**：若最終目標仍是雙平台，「Android 優先、iOS 後續」的分階段總工時約為 22.5 + 19.5 ≈ **42 天**，明顯高於需求書原估的 27 天。這代表原估算若真的意圖涵蓋雙平台，**風險是嚴重低估**；若原估算的本意僅針對單一平台，則本文件的 22.5 天估算與其大致吻合（略低，因已將 F-05/F-08 範圍收斂）。建議在 M0 階段明確與需求書擁有者對齊「27 天估算所指範圍」，避免期望落差。

---

## 八、與 REQUIREMENTS.md 的差異摘要（供對照）

| 項目 | 需求書原文 | 本文件建議 |
|---|---|---|
| 首選方案 | Android 原生 + iOS 原生分開開發（同時） | 先做 Android 原生（單平台 MVP），iOS 待驗證後啟動 |
| F-05 描述 | 可設定跑馬燈捲動速度 | 重新定義為「可設定輪播間隔」（15/30/60 分），並於 UI 文案中管理使用者預期 |
| F-08 定位 | 選擇性 | 選擇性，但因資料源無原生分類欄位，建議明確列為 Phase 3、Phase 1/2 僅做 UI 骨架 |
| M0～M8 工時 | 合計 ~27 天（範圍未標註是否含雙平台） | 單平台（Android）約 22.5 天；若含 iOS 擴展總計約 42 天 |

---

*本文件為需求書之補充分析，聚焦技術選型決策與可執行驗收標準，作為開發與 QA 的依據。若後續資源條件（人力、時程、平台優先序）發生變化，應重新評估第一章之選型結論。*
