# LTDJMS 文件導覽

這份索引用來幫你快速找到「現在該看哪一份文件」。若你是第一次接手這個 repo，先讀根目錄 `README.md`，再依工作情境進入對應主文件。

## 建議閱讀順序

| 步驟 | 你想解決的事 | 文件 |
| --- | --- | --- |
| 1 | 先理解專案是什麼 | `README.md` |
| 2 | 把環境跑起來 | `docs/getting-started.md` |
| 3 | 補齊環境變數與第三方服務 | `docs/configuration.md` |
| 4 | 理解模組邊界與資料流 | `docs/architecture.md` |
| 5 | 快速瀏覽使用者 / 管理員功能 | `docs/features.md` |
| 6 | 準備改程式、測試或除錯 | `docs/developer-guide.md` |

## 依工作情境找文件

### 我要把系統跑起來

- `docs/getting-started.md`：本機啟動、Docker Compose、部署前檢查
- `docs/configuration.md`：`.env`、資料庫、Redis、AI、ECPay、付款 callback / 背景 worker

### 我要理解系統怎麼組成

- `docs/architecture.md`：入口、模組責任、主要流程、事件管線
- `docs/features.md`：從使用者與管理員視角看現有能力

### 我要修改或排查程式

- `docs/developer-guide.md`：高風險區、測試策略、除錯入口
- `docs/api/slash-commands.md`：目前 slash command 與權限、參數整理

## 補充與深度參考

- `docs/api/`：對外互動面參考
- `docs/architecture/`：更細的架構拆解與流程圖
- `docs/modules/`：模組級說明與設計背景
- `docs/development/`：開發細節與測試補充
- `docs/operations/`：維運、監控、效能與故障排查補充

## 閱讀原則

- 先讀根目錄 `README.md` 與 `docs/*.md` 主文件，再進入補充文件。
- 深度文件有些保留了較早期的設計背景；若和主文件或程式碼衝突，以程式碼與主文件為準。
- 設定與行為的最終事實來源是：
  - `src/main/java/ltdjms/discord/shared/EnvironmentConfig.java`
  - `src/main/java/ltdjms/discord/currency/bot/DiscordCurrencyBot.java`
  - `src/main/java/ltdjms/discord/currency/bot/SlashCommandListener.java`
  - 對應模組下的 `services/`、`persistence/`、`commands/`
