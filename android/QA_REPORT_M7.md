# M7 測試報告：歷史上的今天跑馬燈 Widget APP（Android）

- 測試角色：QA 測試員
- 測試日期：2026-07-28
- 測試環境：Windows 11 + Android Studio 模擬器 `emulator-5554`（AVD `timeline_test`，`sdk_gphone64_x86_64`，Google Play Store image）
- 受測建置：`android/app/build/outputs/apk/debug/app-debug.apk`（`./gradlew assembleDebug` 重新確認為最新，`versionName = 0.1.0-m1`）
- 單元測試：`./gradlew testDebugUnitTest` 全部通過，共 43 則測試，0 失敗
- 注意事項：模擬器與另一 App `com.example.weathertool` 共用，測試全程未解除安裝／停用該 App，飛航測試改用 `svc wifi/data disable-enable`（因 shell 無權限廣播 `AIRPLANE_MODE`），測試結束已確認網路狀態（WiFi + Mobile）已還原為連線狀態。

---

## 一、功能測試（F-01～F-08）

| 編號 | 測試項目 | 方法 | 結果 |
|---|---|---|---|
| F-01 | Widget 顯示「【西元年份】事件標題」格式；輪播（定時刷新）機制；無資料時的提示文案 | 實機放置 Widget 觀察、讀 `HistoryWidget.kt`/`WidgetRotationWorker.kt`/`history_widget_loading.xml` | **Pass**。4×1 顯示 `【1356】朱元璋自称吴国公。`格式正確；4×2 顯示年份（粗體）+ 事件文字分行；`HistoryWidgetContent` 對 `event == null` 有明確 `widget_preparing_data` 文案分支，不會空白；輪播由 `WidgetRotationWorker` 週期性 `updateAll()` 驅動，不使用連續動畫，符合平台限制與 ANALYSIS.md 6.1 設計 |
| F-02 | 每日 00:05 排程觸發；失敗重試/退避策略；不清空舊快取 | `adb shell dumpsys jobscheduler` 實際比對 + 讀 `DailyFetchWorker.kt`/`WorkScheduler.kt`/`DailyFetchScheduling.kt` | **Pass**。`dumpsys jobscheduler` 確認 job 已註冊，`Required constraints: TIMING_DELAY CONNECTIVITY`，`Network type: ... INTERNET`，`Minimum latency` 對應到下一次本地 00:05（測試當下約 +17h13m，與執行時間點換算相符）；`Backoff: policy=1(EXPONENTIAL)`；程式碼確認失敗時 `runAttemptCount >= 5` 才 `Result.failure()`，否則 `Result.retry()`；`refreshIfNeeded()` 僅在成功取得非空資料時才 `replaceEventsForDate`，失敗或空回應不會清空舊快取 |
| F-03 | Android 4×1 / 4×2 兩種尺寸顯示；加入/移除；多實例互不干擾 | **本次以實機驗證**：放置 1 個 Widget、手動拖曳調整大小、再放置第 2 個獨立實例 | **Pass**。4×1（180×48dp）與拖曳放大後的 4×2（250×110dp）版面皆正常顯示、無裁切錯亂、無空白（截圖見下）；新增第二個 Widget 實例後 `adb shell dumpsys appwidget` 確認系統確實配發兩個獨立 `AppWidgetId`（`id=4`、`id=5`）；直接讀取 App 私有 Room DB（`run-as` + `sqlite3`）確認 `widget_state` 表以 `widgetId` 為 PK 各自記錄 `currentIndex`，程式碼 `advanceWidgetRotations()` 逐一 widgetId 更新，並有對應單元測試 `WidgetRotationWorkerTest`／`WidgetRotationStateTest` 覆蓋。因每 15 分鐘才輪播一次，未等待真實排程觸發用肉眼看到兩個實例顯示「不同」事件，但資料模型與程式邏輯已充分證明相互獨立 |
| F-04 | iOS Widget | 不適用（Phase 1 未實作，需求書已知） | N/A（略過，符合 ANALYSIS.md 決策） |
| F-05 | 輪播間隔 15/30/60 分鐘設定與生效 | 設定頁實際操作 + `WorkScheduler.updateWidgetRotationInterval()` 程式碼 | **Pass**。設定頁三個選項皆存在、可選取並即時反白；`SettingsViewModel.setRotationIntervalMinutes()` 會呼叫 `updateWidgetRotationInterval()`，以 `ExistingPeriodicWorkPolicy.UPDATE` 立即重新排程；`dumpsys jobscheduler` 確認 WidgetRotationWorker job 無網路限制、`Minimum latency` 與所選間隔相符 |
| F-06 | 點擊 Widget 開啟 App 並導向該事件詳情頁 | 實機點擊 Widget → 觀察前景 Activity + 截圖 | **Pass（含一項 Major 缺陷，見下方缺陷 #2）**。點擊 Widget 後 `dumpsys activity activities` 確認 `MainActivity` 成為 `ResumedActivity`，畫面直接進入該事件的詳情頁（含年份、標題、延伸閱讀連結、Wikipedia CC BY-SA 授權標示），與 Widget 當時顯示內容一致，無崩潰 |
| F-07 | 中／英文切換套用於 UI；事件內容語言 fallback | 設定頁切換語言（中→英→中）+ 截圖比對 | **Pass（含一項 Major 缺陷，見下方缺陷 #1）**。切換語言後 Activity 立即 recreate，list/detail/settings 三頁 UI 文字（含返回鍵頁面標題、選項文字）皆正確套用英文/中文；`values` 與 `values-en` 字串鍵值 100% 一一對應（`diff` 結果為空，無缺漏） |
| F-08 | 分類篩選 UI 骨架 | 讀 `SettingsScreen.kt`/`AppSettings.kt` | **未實作**（依任務指示可略過）。註記：ANALYSIS.md 原文其實要求 Phase 1「UI 骨架存在、預設不啟用」，但目前 `AppSettings` 無 `categoryFilter` 欄位、`SettingsScreen` 無任何分類 UI，屬於比 ANALYSIS.md 規劃更保守的完成度，供未來規劃參考，不計入本次缺陷 |

### F-03 實測截圖說明
- 4×1 版面：`【1356】朱元璋自称吴国公。` 單行顯示，無裁切
- 拖曳放大後 4×2 版面：年份「1356」粗體另起一行、事件文字換行顯示
- 點擊 Widget 開啟詳情頁：畫面完整顯示「西元 1356 年」「朱元璋自称吴国公。」「延伸閱讀」「資料來源：Wikipedia（CC BY-SA 授權）」

---

## 二、非功能需求驗證

| 項目 | 方法 | 結果 |
|---|---|---|
| 離線可用 | `adb shell svc wifi disable` + `svc data disable` 完全斷網後，`force-stop` App 再冷啟動 | **Pass**。斷網情況下 App 冷啟動仍正常顯示先前快取的當日事件清單，無白畫面、無崩潰、無錯誤訊息；測試後已用 `svc wifi/data enable` 還原網路（`dumpsys connectivity` 確認 WIFI 與 MOBILE 皆恢復 CONNECTED，未影響 weathertool 網路環境） |
| 低電耗 | 程式碼審查 + `dumpsys jobscheduler` | **Pass**。全專案未使用 `AlarmManager`/`WakeLock`（grep 結果為空）；`WidgetRotationWorker` 排程無 `NetworkType` 限制且僅讀本機 Room（除非當日完全無快取才 fallback 抓取一次）；`DailyFetchWorker` 週期為 24 小時、`WidgetRotationWorker` 最小週期 15 分鐘，符合 WorkManager 系統下限；`jobscheduler` dump 顯示三個 Job 皆為一般 `PeriodicWorkRequest`／延遲觸發，無高頻喚醒 |
| 隱私 | 檢查 `AndroidManifest.xml`、`libs.versions.toml`、`build.gradle.kts` | **Pass**。`AndroidManifest.xml` 僅宣告 `INTERNET` 權限（無定位/通訊錄/儲存），且刻意不宣告 `RECEIVE_BOOT_COMPLETED`（有 kdoc 說明理由）；全專案 grep 分析/廣告/追蹤 SDK 關鍵字（analytics/firebase/admob/crashlytics/facebook 等）僅命中 `AndroidManifest.xml` 註解本身（說明「不包含」），無實際依賴引入；`allowBackup="false"`，本機資料僅存 Room/DataStore |
| 多語言 | `diff` 比對 `values/strings.xml` 與 `values-en/strings.xml` 鍵值 | **Pass（但發現 1 項 Minor 缺陷，見下方缺陷 #3）**。兩份字串資源鍵值 100% 對應，無缺漏字串；UI 實測中英文切換皆無文字截斷或亂碼。惟事件「內容」文字（非 UI 字串）在切換至英文後仍顯示簡體中文，詳見缺陷 #1 與 #3 |

---

## 三、資料來源穩定性測試（Wikipedia API 容錯）

- 透過離線測試間接驗證：中斷網路後 `refreshIfNeeded()` 於 `IOException`/`HttpException` 時回傳 `Result.failure`，不清空既有快取（程式碼確認 + 離線冷啟動實測一致）。
- 讀 `HistoryEventRepositoryImpl.refreshIfNeeded()`：對 API 回應做 `filter { it.text != null }` 防呆過濾，空/畸形回應不會覆蓋既有快取，僅在 `entities.isNotEmpty()` 才寫入。
- `UserAgentInterceptor` 已依 Wikimedia API 規範帶上具聯絡資訊的 User-Agent（含 GitHub repo + 開發者 email placeholder），降低被限流風險。
- 未實際模擬「API 回應格式變更」情境（例如竄改 JSON schema），此項僅能以程式碼審查方式確認 Moshi 解析為寬鬆的 nullable 欄位設計（`EventDto.text`/`year`/`pages` 皆可空），推斷格式輕微變動不至於直接崩潰，但未做逾時（timeout）情境的實測（已於程式碼確認 timeout 為 10 秒）。

---

## 四、輪播技術限制驗證（需求書附錄）

- `WorkScheduler.MIN_INTERVAL_MINUTES = 15`，且 `scheduleWidgetRotation`/`updateWidgetRotationInterval` 皆用 `intervalMinutes.coerceAtLeast(15)`，符合 WorkManager 15 分鐘下限與需求書附錄的「15–30 分鐘」設計。
- 輪播內容不重複／不遺漏：`WidgetRotationState.nextIndex()` 為簡單環狀遞增 `(current + 1) % eventCount`，並有單元測試 `WidgetRotationStateTest` 覆蓋邊界（0 筆、1 筆、多筆、越界 index）。
- 多實例輪播互相獨立：見上方 F-03 說明（`dumpsys appwidget` + Room DB 直接查詢佐證）。
- iOS WidgetKit Timeline 對應機制：不適用（Phase 1 未實作）。

---

## 五、例外／邊界情境測試（logcat 監控）

測試操作涵蓋：App 冷啟動、離線冷啟動、List→Detail→返回、Widget→Detail（含返回鍵）、新增/調整/新增第二個 Widget、語言切換（中→英→中）、輪播間隔切換、`force-stop` 後重啟。

**全程 `adb logcat -d | grep "FATAL\|AndroidRuntime"` 未發現任何 App 相關的 crash 或 ANR。** 无崩潰記錄。

---

## 六、缺陷清單

### 缺陷 #1（Major）— F-07 事件「內容」不會隨語言設定切換為英文

- **重現步驟**：
  1. 開啟 App → 設定頁 → 語言選擇「English」
  2. 返回列表頁
- **預期結果**（依 ANALYSIS.md 四 F-07 AC1/AC3、REQUIREMENTS.md F-07「可設定事件語言（中文／英文）」）：事件內容文字應顯示英文版（若英文版無對應資料才 fallback 顯示中文，且不留空白）
- **實際結果**：UI chrome（標題列「On This Day」、按鈕文字等）正確切換為英文，但每一則歷史事件的內容文字仍固定顯示簡體中文（例如「【1356】朱元璋自称吴国公。」，即使已切換為 English）
- **根因（程式碼確認）**：`di/NetworkModule.kt` 確實建立了 `@EnWikipediaApi` 的 Retrofit/Api 實例，但全專案 grep `EnWikipediaApi`/`enApi` 使用點，除了 DI 宣告本身外，**沒有任何呼叫端實際注入或呼叫這個英文 API**。`HistoryEventRepositoryImpl.refreshIfNeeded()` 建構式僅注入 `@ZhWikipediaApi` 的 `zhApi`，且固定 `language = EventLanguage.ZH` 呼叫 `toEntity()`，導致 `HistoryEventEntity.textEn` 永遠是 `null`。`HistoryEventUiModel` 的 fallback 邏輯 `textZh ?: textEn` 因此永遠解析為中文文字。
- **嚴重程度**：Major（明確寫在驗收條件中的功能未達成，但因有 fallback 不會顯示空白/崩潰，不到 Blocker）
- **建議**：於 `HistoryEventRepositoryImpl` 依目前語言設定（或同時）呼叫英文 API 並將結果寫入 `textEn` 欄位；或至少在 M7/M8 前與需求方確認是否要下修此驗收條件範圍。
- **修復狀態：已於本輪迴歸驗證確認修復，詳見九、缺陷修復迴歸驗證。**

### 缺陷 #2（Major）— 透過 Widget 開啟詳情頁後，畫面上的「←」返回鍵是無效的死按鈕

- **重現步驟**：
  1. 完全結束 App（`force-stop`，確保無現有 Task）
  2. 點擊主畫面 Widget 目前顯示的事件
  3. App 開啟並直接進入該事件詳情頁（正常，F-06 預期行為）
  4. 點擊畫面左上角的「←」返回鍵
- **預期結果**：應返回列表頁（或至少有可視的畫面變化）
- **實際結果**：畫面完全沒有反應，仍停留在原詳情頁（`dumpsys activity`確認 Activity 未改變）。若改按系統返回鍵（手勢/按鍵），則是直接整個結束 App 回到桌面，同樣無法看到列表頁。
- **根因（程式碼確認）**：`MainActivity.kt` 的 `OnThisDayNavHost` 在偵測到 Widget 傳入的 `eventId` 時，直接把 `NavHost` 的 `startDestination` 設為 `DETAIL_ROUTE`（列表頁完全不在返回堆疊中）。`HistoryDetailScreen` 的 `onBack` 呼叫 `navController.popBackStack()`，但因為目前畫面本來就是 `startDestination`、沒有上一頁可退，`popBackStack()` 靜默失敗、不會有任何效果——而按鈕仍然顯示為可點擊、看起來像正常返回鍵。
- **對照組（已驗證正常）**：從列表頁正常點擊某事件進入詳情頁，此時「←」按鈕可以正常返回列表頁，問題僅限於「透過 Widget 深連結進入」這條路徑。
- **嚴重程度**：Major（是本次任務明確要求驗證的 F-06 使用者流程的一部分；雖非崩潰，但是一個看起來可互動、實際上完全無反應的畫面元素，使用者會誤以為 App 當機或自己操作有誤）
- **建議**：Widget 深連結進入詳情頁時，應把列表頁一併加入 NavHost 的起始堆疊（例如 `navController.navigate` 兩步：先 List 再 push Detail），或讓 `onBack` 在沒有上一頁時改為 `finish()`／導回列表頁而非靜默失敗。
- **修復狀態：已於本輪迴歸驗證確認修復，詳見九、缺陷修復迴歸驗證。**

### 缺陷 #3（Minor）— 事件內容為簡體中文，與 App 宣稱的「繁體中文」UI 不一致

- **重現步驟**：語言設定維持預設「繁體中文」，直接查看列表頁任一事件內容
- **預期結果**：事件內容文字字型應與 UI 語言一致，顯示繁體中文（例如「稱」「國」）
- **實際結果**：事件內容文字實際上是簡體字（例如「朱元璋自称吴国公」中的「称」「国」皆為簡體），與 App 標題「歷史上的今天」及其餘 UI 文字的繁體字型不一致
- **根因**：`WikipediaOnThisDayApi`/`UserAgentInterceptor` 呼叫 `zh.wikipedia.org` 端點時未帶任何語言變體協商（例如 MediaWiki 的 `Accept-Language: zh-Hant` 或對應 URL variant 參數），中文維基百科 REST API 在無指定時似乎預設回傳簡體語言變體
- **嚴重程度**：Minor（不影響功能，但影響「多語言」非功能需求的呈現品質與使用者觀感，尤其台灣/香港用戶可能認為是文字亂碼或翻譯錯誤）
- **建議**：於 API 請求加上繁體中文變體協商（`Accept-Language: zh-Hant` header，或改用 `zh-tw.wikipedia.org`／查詢參數），並在 M8 上架前一併確認 App Store 截圖與敘述文字的語言一致性
- **修復狀態：已於本輪迴歸驗證確認修復，詳見九、缺陷修復迴歸驗證。**

### 缺陷 #4（Minor）— Widget 選單預覽圖為系統預設機器人圖示，非實際 Widget 外觀

- **重現步驟**：主畫面長按空白處 → 小工具 → 捲動至「On This Day」
- **預期結果**：預覽圖應呈現實際 Widget 版面（例如年份+事件文字的縮圖）
- **實際結果**：預覽圖僅顯示系統預設的白底 Android 機器人圖示，且下方標示的尺寸為「3 × 1」，與 `history_widget_info.xml` 宣告的 `targetCellWidth="4"` 不完全一致（推測為不同啟動器格線換算差異，非崩潰性問題）
- **根因**：`history_widget_info.xml` 未設定 `android:previewImage`（或 Android 12+ 的 `previewLayout`）
- **嚴重程度**：Minor（僅影響上架前的「Widget 預覽圖是否符合實際顯示」檢查項目，實際放置後的 Widget 顯示本身完全正常，已於本報告 F-03 驗證）
- **建議**：M8 上架前補上 `previewImage`/`previewLayout`，並實機截圖核對 Google Play 商店頁面所需的 Widget 預覽素材
- **修復狀態：本輪未修復（開發者未聲稱修復此項），本輪迴歸測試中在 Widget 選單再次確認仍為系統預設機器人圖示、尺寸標示仍為「3 × 1」，狀態不變，維持 M8 前補上即可，非阻斷項。**

### 缺陷 #5（Minor / Low，僅程式碼審查發現，未實測重現）— 移除 Widget 實例時未清除對應的 `widget_state` 資料列

- **說明**：`HistoryWidgetReceiver` 未覆寫 `onDeleted()`/`onDisabled()` 來刪除被移除 Widget 實例對應的 `WidgetStateEntity` 資料列，長期下來若使用者反覆新增/移除 Widget，`widget_state` 表會累積孤兒資料（實務影響極小，每列僅 3 個 int/long 欄位）
- **嚴重程度**：Low（不影響功能與效能，僅為程式碼整潔度問題，非阻斷項）
- **建議**：可於 M8 前補上（非必要）；不影響本次上架建議
- **修復狀態：程式碼審查確認已修復（`onDeleted()` 已補上），但本輪未能以真正的系統級「移除 Widget」路徑端對端驗證，詳見九、4 節的說明與限制。**

---

## 七、上架前檢查清單（M8 準備度，初步）

| 項目 | 狀態 | 備註 |
|---|---|---|
| 權限最小化 | ✅ | 僅 `INTERNET` |
| 無第三方追蹤/廣告 SDK | ✅ | grep 確認 |
| 隱私權政策揭露 | ⚠️ 待補 | 目前專案內未見隱私權政策頁面/連結，Google Play 上架需要隱私權政策 URL（即使不蒐集個資，仍建議準備一頁簡短聲明） |
| CC BY-SA 資料來源標註 | ✅ | 詳情頁已有「資料來源：Wikipedia（CC BY-SA 授權）」字樣 |
| Widget 預覽素材 | ⚠️ 待補 | 見缺陷 #4，商店上架用截圖建議直接用實機截圖而非依賴系統預設預覽 |
| 崩潰穩定性 | ✅ | 本次測試全程無 FATAL/ANR |
| 離線可用性 | ✅ | 已實測 |
| F-07 語言切換完整度 | ❌ 待修 | 見缺陷 #1，建議修復或明確調整驗收範圍後再上架，避免上架後被使用者回報「英文模式看不到英文內容」 |
| F-06 返回導覽 | ❌ 待修 | 見缺陷 #2，屬於核心互動路徑（Widget 為本 App 主打功能）的可用性缺陷，建議修復後再上架 |

---

## 八、整體品質判斷與建議

M7 測試範圍內：**8 項功能需求中已實作的 6 項（F-01/02/03/05/06/07）大方向皆可運作、非功能需求（離線／低電耗／隱私／多語言字串）全數通過、全程無任何崩潰或 ANR**，Debug 建置與 43 則單元測試皆為綠燈，整體工程基本功紮實，M1～M6 的骨架與排程機制設計合理（尤其 `dumpsys jobscheduler` 實測結果與程式碼描述的排程策略完全吻合，殊為難得）。

但本輪測試找到 **2 項 Major 缺陷（F-07 事件內容不隨語言切換、F-06 Widget 深連結進入後返回鍵失效）**，兩者都直接對應到需求書「建議」等級但已明確承諾的驗收條件，且都是使用者會在極其常見的操作路徑中立即碰到的問題（並非邊界情境）。此外還有 3 項 Minor 缺陷（簡繁字型不一致、Widget 預覽圖、資料表孤兒列清理）。

**建議：暫緩進入 M8 上架，先修復兩項 Major 缺陷（#1、#2）。** 這兩項缺陷雖不會導致當機，但都會讓真實使用者在第一次使用「語言切換」或「點 Widget 看詳情」這兩個核心賣點功能時，立刻感受到「壞掉了」的觀感（英文模式看到全中文內容、點返回鍵沒反應）。3 項 Minor 缺陷可視工時彈性安排於 M8 前後修復，不建議作為上架阻斷項。修復 #1、#2 並補一輪迴歸測試後，本 App 應可具備進入 M8 上架準備的品質水準。

---

## 九、缺陷修復迴歸驗證

- 測試角色：QA 測試員（獨立驗證，非開發者自我回報）
- 驗證日期：2026-07-29
- 測試環境：同上（`emulator-5554` / AVD `timeline_test`）。**本輪開始時模擬器處於未啟動狀態**（`adb devices` 回傳空清單、`tasklist` 未見任何 emulator/qemu 行程），判斷為單純關機而非當機，故以 `emulator -avd timeline_test -no-snapshot-save` 重新啟動（並未執行 factory reset / wipe-data，僅正常開機），開機完成後確認 `com.timeline.onthisday` 與 `com.example.weathertool` 兩個 App 皆完整保留、未被解除安裝。
- 受測建置：重新 `adb install -r` 安裝 `android/app/build/outputs/apk/debug/app-debug.apk`。以 `find app/src/main -name "*.kt" -newer <apk>` 確認專案中沒有任何原始碼檔案比這顆 APK 新，即這顆 APK 確實對應開發者聲稱的修復後程式碼（並非舊版殘留）。舊版 App（`lastUpdateTime=2026-07-28 07:31:47`，即 DB schema version=1 的舊安裝）被覆蓋安裝為新版，因此本次冷啟動天然涵蓋了「Room `fallbackToDestructiveMigration()` 從 version 1 升級到 version 2」的實際路徑，而非乾淨全新安裝。

### 1. 缺陷 #1（Major：語言切換內容不隨語言變化）—— 確認修復

- **語言互切內容驗證（中→英→中）**：實機截圖比對，中文列表（【-243】秦國發生蝗災…、【713】唐玄宗與宰相郭元振…等）與英文列表（【-587】The Neo-Babylonian Empire sacks Jerusalem…、【238】The Praetorian guard revolts…等）為**完全不同的事件集合**，並非同一批事件的逐句翻譯——與開發者聲稱的「zh/en 各自獨立策展」資料模型一致，非缺陷 #1 原本「英文模式仍顯示中文內容」的症狀。往返切換中→英→中皆正確套用對應語言，UI chrome 與事件內容同步切換，未出現卡在單一語言的情形。
- **「剛切換到尚無本地快取的語言」情境（本次任務重點）**：以 `run-as` + `sqlite3` 手動清空 `history_event` 表中 `language='EN'` 的所有列（模擬英文從未被抓取過），並以 `svc wifi/data disable` 完全斷網，此時在設定頁切至 English：畫面**未出現白畫面、未崩潰、也未顯示舊的中文資料充數**，而是正確呈現 `HistoryListUiState.Empty(isError=true)` 對應的英文文案「Couldn't load data — check your connection and try again」+「Retry」按鈕（UI 語言仍是剛切換的 English，未卡在中文）。恢復網路後點擊「Retry」，成功重新從 `en.wikipedia.org` 抓取並正確顯示英文事件列表。此行為符合 `HistoryListViewModel`「語言切換即 `flatMapLatest` 到該語言獨立 Flow + 觸發該語言 `refresh()`」的設計，且與 ANALYSIS.md 五「首次使用受限」情境的預期提示一致，未發現迴歸。
- **結論：Pass，缺陷 #1 修復有效，且邊界情境（無快取＋離線）表現符合預期。**

### 2. 缺陷 #2（Major：Widget 深連結返回鍵失效）—— 確認修復

- **方法說明（環境限制）**：本輪嘗試透過 adb 自動化「長按主畫面 → 小工具 → 拖曳 On This Day 小工具到主畫面」以取得一個真正的 Widget 實例可供點擊，但 Pixel Launcher 的小工具拖放需要「長按停留 → 進入拖曳態 → 移動 → 放開」的連續手勢，`adb shell input swipe`／`input draganddrop` 皆無法正確觸發拖曳（會被判定為一般點擊，導致小工具選單直接關閉、未加入任何實例；`dumpsys appwidget` 全程 `widgets.size=0`）。因此改採**功能等價**的驗證方式：直接以 `adb shell am start -n com.timeline.onthisday/.MainActivity --el eventId <id>` 送出與 `HistoryWidget.kt` 內 `actionStartActivity<MainActivity>(parameters = actionParametersOf(HistoryWidget.EVENT_ID_KEY to event.id))` 完全相同的 Intent（同一 Activity、同一 extra key `"eventId"`），這與真正點擊 Widget 觸發的下游程式碼路徑（`MainActivity.onCreate` 讀取 `eventId` extra → `OnThisDayNavHost`）完全一致，僅省略了「小工具如何被放上主畫面」這一段與缺陷#2 修復內容無關的 UI 步驟。
- **重現步驟與結果**：
  1. `am force-stop com.timeline.onthisday`（確保無殘留 Task）
  2. `am start ... --el eventId 111`（模擬點擊 Widget）→ 直接進入該事件詳情頁（`Event Detail` / 年份 2019 / 中文事件內容），`dumpsys activity` 確認為新 Task
  3. 點擊左上角「←」→ **成功返回列表頁**（`On This Day` 標題重新出現，`dumpsys activity` 確認仍是同一 `MainActivity`／`t35`，並未整個結束 App）
- **對照組（列表→詳情→返回，確認未被此次改動破壞）**：於列表頁點擊任一事件卡片 → 正常進入詳情頁 → 點「←」→ 正常返回列表頁，行為與修復前 QA_REPORT 記錄的「已驗證正常」路徑一致，無迴歸。
- **結論：Pass，缺陷 #2 修復有效（`[List, Detail]` 回退堆疊確認生效），且原本正常的路徑未被此次改動破壞。**

### 3. 迴歸測試範圍

| 項目 | 方法 | 結果 |
|---|---|---|
| F-01（Widget 顯示格式／輪播） | 因上述小工具拖放自動化限制，本輪**未能在真正的主畫面 Widget 實例上重新肉眼確認顯示畫面**；改以程式碼審查確認 `HistoryWidget.kt`／`WidgetRotationWorker.kt` 已正確改為依 `settingsRepository.currentSettings().language` 查詢 `language`-scoped 的 Room 資料（`historyEventDao.getEventsForDateOnce(month, day, language.name)`），schema 由 `textZh`/`textEn` 改為 `language`/`text` 後查詢邏輯與 UI 顯示欄位（`entity.text`）並未遺漏或誤用舊欄位名。**建議標記為「程式碼審查通過、實機肉眼複驗待補」**，非本輪的高信心 Pass。 |
| F-02／F-03（背景排程／多 Widget 獨立） | `adb shell dumpsys jobscheduler` 實測 | **Pass**。三個 Job 皆仍正常註冊：`WidgetRotationWorker`（`Minimum latency: +17m43s`，與目前設定的 30 分鐘輪播間隔換算相符）、`DailyFetchWorker`（`Minimum latency: +23h47m48s`，對應下一次本地 00:05）、以及 WorkManager 內部維護性 Job（`charging=true` 的長延遲 Job，非本專案程式碼建立，屬 WorkManager 標準行為）。`WidgetRotationWorker.kt` 程式碼審查確認 `advanceWidgetRotations()` 仍以 `widgetId` 為 key 逐一更新 `widget_state`，多實例獨立邏輯本身未被此次資料層重構影響；因同一小工具拖放限制，本輪未能新增第二個實機 Widget 實例重新肉眼確認互不干擾，此點沿用 M7 原報告的程式碼佐證結論。 |
| F-05（輪播間隔設定） | 設定頁實際操作 | **Pass**。設定頁「Widget 輪播間隔」三選項正常顯示、可選取，目前為「30 分鐘／30 minutes」，且與 `dumpsys jobscheduler` 的 `WidgetRotationWorker` 排程延遲換算一致，切換語言後此區塊文字與選取狀態亦正確跟著切換語言（無跨語言遺失設定的情形）。 |
| 離線可用（本輪風險最高項） | `svc wifi/data disable` 完全斷網後 `force-stop` + 冷啟動（重新安裝新 schema 後、且已有 EN/ZH 雙語快取的狀態下） | **Pass**。斷網冷啟動後正確顯示先前快取的英文事件列表（當時語言設定為 English），無白畫面、無錯誤訊息、無崩潰；`adb logcat -d` 掃描全程無 `FATAL`/`AndroidRuntime` 例外。測試後已以 `svc wifi/data enable` 還原網路，`dumpsys connectivity` 確認 WIFI 與 MOBILE 皆恢復 `CONNECTED`，`com.example.weathertool` 網路環境未受影響。 |

### 4. Minor #3／#5 驗證

- **Minor #3（簡體字內容）**：實機截圖確認中文事件內容已改為繁體字（例如「拜占庭帝國」「擊敗」「艦隊」「戰爭」「軍隊」等皆為繁體寫法，非簡體的「击败」「战争」「军队」），與 `HistoryEventRepositoryImpl` 程式碼中對中文請求帶 `Accept-Language: zh-Hant` 的修復描述一致。**結論：Pass，已修復。**
- **Minor #5（移除 Widget 未清資料）**：
  - 程式碼審查：`HistoryWidgetReceiver.onDeleted()` 已正確覆寫，對傳入的每個 `appWidgetIds` 呼叫 `widgetStateDao.deleteState(widgetId)`（對應 `WidgetStateDao` 既有的 `DELETE FROM widget_state WHERE widgetId = :widgetId` 查詢），程式邏輯與開發者聲稱的修復內容相符。
  - 端對端實測受限：本輪嘗試以 `run-as`＋`sqlite3` 手動插入一筆假 `widget_state` 列（`widgetId=9999`）後，嘗試以 `adb shell am broadcast -a android.appwidget.action.APPWIDGET_DELETED` 直接觸發 `HistoryWidgetReceiver.onDeleted()`，但被系統拒絕（`SecurityException: Permission Denial: not allowed to send broadcast ... from pid=... uid=2000`，`APPWIDGET_DELETED` 為系統保護廣播，adb shell 身分不允許手動送出）；又因前述小工具拖放自動化限制，也無法透過「真正在主畫面新增後移除 Widget」的方式觸發。已將測試用的假資料列清除，未留下殘留。
  - **結論：僅完成程式碼審查層級的確認（判定與開發者描述相符），未能完成真正的系統級端對端驗證，建議後續若有真機或可操作的自動化工具（例如 UI Automator / Espresso 搭配實機小工具拖放），補一次端對端驗證再視為完全關閉。**

### 5. logcat 崩潰掃描

- 涵蓋整個迴歸測試過程（重新安裝＋DB migration、語言切換×多次、離線＋無快取語言切換、Widget 深連結模擬×2、列表正常導覽、離線冷啟動、Retry 流程）：`adb logcat -d | grep -i "FATAL\|AndroidRuntime"` 與 `adb logcat -d -b crash` 均**未發現任何與 `com.timeline.onthisday` 相關的 crash 或 ANR**。DB schema version 1→2 的 `fallbackToDestructiveMigration()` 路徑本身也未觸發任何例外。

### 6. `fallbackToDestructiveMigration()` 合理性留意事項

- `AppDatabase.kt` 目前 `exportSchema = false`（M1 即如此設計，未變更），版本由 1 bump 到 2 時採用 `fallbackToDestructiveMigration()` 而非撰寫真正的 `Migration`，程式碼註解已明確說明理由：「pre-M8/unreleased，沒有真實使用者資料，本機快取重新從網路抓取即可、成本低且必然正確」。**在目前「尚未上架、無真實使用者」的階段，這個判斷合理，本輪不視為缺陷或阻斷項。**
- 留意事項（供 M8 前 / 未來提醒，非本輪缺陷）：一旦上架後有真實使用者安裝過 App，`fallbackToDestructiveMigration()` 會在下次 schema 變更時**無預警清空使用者本機的 Room 快取**（含 `widget_state` 輪播進度）。建議在下一次真的需要動 schema 前（尤其是上架後），評估是否該補上 `exportSchema = true` 並提交 schema JSON 到版控、改寫真正的 `Migration`，或至少確認「清空快取＋重新抓取」在當時的網路情境下仍是可接受的使用者體驗（例如離線時清空快取會讓使用者暫時看到空清單，直到下次連網）。

### 7. 本輪結論

兩項 Major 缺陷（#1 語言切換內容不隨語言變化、#2 Widget 深連結返回鍵失效）**均已獨立驗證確認修復**，且針對本次任務特別要求的高風險場景（語言切換至尚無快取時的首次載入體驗、DB schema 變更後的離線可用性）皆通過測試，未發現新的崩潰或明顯迴歸。Minor #3（簡體字）確認修復；Minor #5（widget_state 孤兒列）程式碼審查通過但受工具限制未能端對端驗證；Minor #4（Widget 預覽圖）維持原狀、未修復（開發者本輪未聲稱處理，非阻斷項）。

**主要保留項**：F-01／F-03 這次未能在真正放置於主畫面的 Widget 實例上重新肉眼複驗（僅程式碼審查），原因是本測試環境下 adb 自動化無法可靠模擬 Pixel Launcher 的小工具拖放手勢；建議若有真機或人工操作機會，補一次「實際放置 Widget → 肉眼確認 4×1/4×2 顯示與內容」的複驗，再完全視為無風險。在此保留項之外，**建議可進入 M8 上架準備**；`fallbackToDestructiveMigration()` 的長期風險已於上方記錄，屬於「留意但非阻斷」等級。

---

## 十、使用者實測發現之新缺陷與修復（2026-07-29）

- 回報來源：**真人操作**（使用者本人手動把 Widget 拖到模擬器主畫面並點擊，是本 App 第一次被真正的手動點擊測試，非 adb 模擬）。
- 修復角色：開發工程師
- 修復日期：2026-07-29
- 測試環境：同上（`emulator-5554` / AVD `timeline_test`），本輪開始時模擬器已在執行中，主畫面上已有使用者手動放置的一個真實 Widget 實例（`dumpsys appwidget` 確認 `id=3`／`id=29` provider 記錄），全程未重新拖曳或新增 Widget，直接沿用該實例；`com.example.weathertool` 全程未被解除安裝/停用/清資料，測試結束後重新確認其套件與網路狀態（WiFi + Mobile CONNECTED）皆未受影響。

### 缺陷 #6（Major）— App 在背景時點擊 Widget，會疊出第二個 `MainActivity` 實例，導致返回鍵失靈

- **重現步驟**：
  1. 正常啟動 App（非 `force-stop` 後的冷啟動）
  2. 按 Home 鍵讓 App 退到背景（不殺掉行程）
  3. 點擊主畫面上的 Widget（目前顯示中的事件）
- **預期結果**：App 前景化並導向該事件詳情頁，同一個 `MainActivity` 實例應被重用（如同 M7 缺陷 #2 已驗證過的「返回鍵可正常運作」路徑）
- **實際結果**：`adb shell dumpsys activity activities` 確認同一個 Task 內疊出**兩個** `MainActivity` 實例（`numActivities=2`）：
  ```
  Task{... A=10204:com.timeline.onthisday} numActivities=2
  * Hist #1: ... cmp=com.timeline.onthisday/.MainActivity ... Intent { dat=glance-action:/CALLBACK?appWidgetId=3... }
  * Hist #0: ... cmp=com.timeline.onthisday/.MainActivity ... Intent { act=android.intent.action.MAIN ... }
  ```
  按返回鍵時，第一次退出的是最上層新疊的實例（其內部 NavHost 從 Detail 退到 List 又整個退出），退出後底下還藏著一個舊的 `MainActivity` 實例，使用者會感覺「按了好幾次都回不到主畫面」／卡住。
- **根因**：`MainActivity` 的 `launchMode` 為預設值 `standard`。Widget 透過 Glance 的 `actionStartActivity` 開啟 App 時，若 `MainActivity` 已有一個實例存活在背景（App 只是被 Home 鍵退到背景，並非完全冷啟動），系統會直接在同一個 Task 上再疊一個新的 `MainActivity` 實例，而不是重用既有實例並呼叫 `onNewIntent()` 傳遞新的 `eventId`。
  - M7 原報告缺陷 #2 的修復與迴歸驗證，測試手法皆是先 `am force-stop` 確保完全冷啟動再點 Widget，這種情境下系統本來就只會有一個乾淨的 `MainActivity` 實例，因此完全沒有覆蓋到「App 已在背景時點 Widget」這條最常見的真實使用路徑，才會被使用者的真人操作實測踩到。
- **嚴重程度**：Major（Widget 為本 App 核心賣點功能，「App 在背景時點 Widget」是比「完全冷啟動後點 Widget」更常見的真實使用情境）

**修復方式**：

1. `AndroidManifest.xml`：`MainActivity` 加上 `android:launchMode="singleTop"`——當 `MainActivity` 已經是 Task 頂端的活動實例時，系統改為呼叫 `onNewIntent()` 重新投遞 Intent，而非建立新實例。
2. `MainActivity.kt`：
   - 新增 `pendingWidgetNavigation`（`mutableStateOf<WidgetNavigationRequest?>`，`WidgetNavigationRequest` 含 `eventId` 與遞增的 `requestId`），由 `onCreate`（冷啟動路徑）與新覆寫的 `onNewIntent()`（singleTop 重用路徑）共同寫入，取代原本只在 `onCreate` 建構式時期讀取一次的做法。
   - `OnThisDayNavHost` 改為讀取這個可觀察的 state，`LaunchedEffect(pendingNavigation)` 在其變化時觸發導覽，確保重複點擊 Widget（含顯示不同 `eventId`、甚至重複點同一則事件）都能正確再次導到最新的事件詳情頁。
   - **一併修復的連帶缺陷**：修復過程中在真機驗證時另外發現，若不做額外處理，每次 `onNewIntent` 觸發的 `navController.navigate(...)` 都會在 NavHost 的 back stack 上再疊一層新的 `Detail` 路由（因為 `navigate()` 預設是疊加而非取代），導致連續點擊 Widget N 次後，需要按 N+1 次返回鍵才能完全退出 App——雖然不是使用者原始回報的症狀，但性質相同（回不去主畫面／需要按很多次返回鍵），一併在本次修復中處理：`navigate()` 呼叫加上 `popUpTo(AppDestinations.LIST_ROUTE) { inclusive = false }` + `launchSingleTop = true`，確保無論點擊 Widget 幾次，back stack 永遠是 `[List, Detail]`，返回鍵固定兩次即可完全退出。
3. **已知未涵蓋的邊緣情境**（記錄但不阻斷本次修復）：若 `MainActivity` 存在於 Task 中但**不在 Task 頂端**（例如使用者從 App 切去別的 App 又切回，中途沒有殺掉 `MainActivity`，但目前 Task 頂端不是它），`singleTop` 不會觸發、系統仍可能建立新實例。查證 Glance 目前的 `actionStartActivity` API 並未提供直接附加 `FLAG_ACTIVITY_CLEAR_TOP` 等 Intent flags 的參數，因此這個更邊緣的情境暫不處理，留待未來若真的造成困擾再評估（例如改用 `PendingIntent` 手動組裝 Intent flags）。

### 驗證結果（皆為「App 在背景時點 Widget」情境，非冷啟動）

在模擬器既有的真實 Widget 實例上，以 `adb shell input tap` 直接點擊該 Widget（而非用等效 Intent 模擬，取得比 M7 缺陷 #2 迴歸驗證當時更高的真實度；小工具本身沿用使用者已放置好的實例，未重新拖曳）：

| 情境 | 方法 | 結果 |
|---|---|---|
| 正常啟動 → Home 退到背景 → 點 Widget | `am start`（一般啟動，非 force-stop 後）→ `input keyevent HOME`（`ps` 確認行程仍存活）→ `input tap` 點擊 Widget | **Pass**。`dumpsys activity activities` 確認同一 Task 內只有 **一個** `MainActivity` 實例（同一 ActivityRecord token，`Hist #0` 僅一筆），正確導向 Widget 當時顯示的事件詳情頁（截圖確認年份/內容與 Widget 一致） |
| 返回鍵：一次回列表、二次退出 App | 連續按兩次 `KEYCODE_BACK` | **Pass**。第一次返回正確顯示列表頁（screenshot 確認「歷史上的今天」列表標題與事件卡片），第二次返回後 `dumpsys activity activities` 對該 Task 查無結果（Task 已完全移除），非卡住、非跳出其他殘留畫面 |
| App 在背景時連續點擊 Widget 兩次 | 點 Widget → 開詳情頁 → 按 Home 退到背景（不殺行程）→ 再點一次 Widget | **Pass**。兩次點擊全程僅有同一個 `MainActivity` 實例（同一 ActivityRecord token），未疊出第二個實例；修復連帶問題後，返回鍵固定兩次即可完全退出（未修復前，此情境下需要三次返回鍵才能退出，已透過 `popUpTo`/`launchSingleTop` 修正） |
| 完全冷啟動（`force-stop` 後點 Widget）未被破壞 | `am force-stop` → `input tap` 點 Widget | **Pass**。行為與 M7 原報告一致：正確導向詳情頁、同一 Task 僅一個 `MainActivity` 實例、返回鍵固定兩次可完全退出，未見任何迴歸 |
| Widget 實例、`weathertool`、網路狀態 | `dumpsys appwidget` / `pm list packages` / `dumpsys connectivity` | **Pass**。測試全程未新增/移除/拖曳 Widget，原有實例（provider `HistoryWidgetReceiver`）自始至終存在；`com.example.weathertool` 全程保持已安裝狀態；WiFi 與 Mobile 網路皆維持 `CONNECTED`，測試過程未曾停用網路 |
| Crash / ANR 掃描 | `adb logcat -d \| grep -i "FATAL\|AndroidRuntime"`（過濾 `timeline`） | 全程無與 `com.timeline.onthisday` 相關的崩潰或 ANR 紀錄 |

### `./gradlew build` 狀態

- 修復第一版（`singleTop` + `onNewIntent`，尚未加 `popUpTo`/`launchSingleTop`）：`BUILD SUCCESSFUL`。
- 加上 `popUpTo`/`launchSingleTop` 連帶修復後：重新執行 `./gradlew build`，`BUILD SUCCESSFUL in 1m 57s`，`119 actionable tasks: 38 executed, 81 up-to-date`；`app/build/test-results/testDebugUnitTest` 下加總確認 **46 則單元測試**全數存在且對應建置成功（既有測試數量與 M7 原報告一致，未減少、未新增測試，本次修復純屬 Activity/Navigation 邏輯調整，未新增可獨立單元測試的邏輯單元）。
- JDK：以 `JAVA_HOME` 指向 `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`（JDK 17）建置，未更動任何全域環境變數設定。

### 結論

缺陷 #6（App 在背景時點 Widget 疊出兩個 `MainActivity` 實例、返回鍵失靈）**已修復並驗證**，測試方式改為在真實 Widget 實例上直接點擊（而非等效 Intent 模擬），涵蓋「一般啟動＋退到背景」「背景時連續點兩次」「完全冷啟動」三種情境，且已確認未破壞既有的冷啟動路徑與 M7 缺陷 #2 的修復成果。過程中額外發現並一併修復了一個性質相近、但使用者尚未實際回報的連帶缺陷（NavHost back stack 隨重複點擊 Widget 無限疊加，需按多次返回鍵才能退出）。已知限制：`MainActivity` 存在但不在 Task 頂端這種更邊緣的情境（`singleTop` 對此無效），因 Glance `actionStartActivity` 目前不支援附加 Intent flags，暫不處理，留作已知限制記錄。
