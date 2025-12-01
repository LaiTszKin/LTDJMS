# Slash Commands 參考文件

本文件列出 LTDJMS Discord Bot 目前註冊的所有 slash commands，包含用途、權限需求、參數與範例。

指令定義可在 `src/main/java/ltdjms/discord/currency/bot/SlashCommandListener.java` 中找到，本文件是該程式行為的文件化版本。

## 指令總覽

| 指令 | 權限 | 模組 | 說明 |
|------|------|------|------|
| `/balance` | 所有成員 | 貨幣系統 | 查看自己的伺服器貨幣餘額 |
| `/currency-config` | 管理員 | 貨幣系統 | 設定或查看伺服器貨幣名稱與圖示 |
| `/adjust-balance` | 管理員 | 貨幣系統 | 調整成員的貨幣餘額（加、減或設定為指定值） |
| `/game-token-adjust` | 管理員 | 遊戲代幣 | 調整成員的遊戲代幣餘額 |
| `/dice-game-1` | 所有成員 | 小遊戲 | 使用遊戲代幣玩骰子遊戲 1，依骰子點數獲得貨幣獎勵 |
| `/dice-game-1-config` | 管理員 | 小遊戲 | 設定或查看骰子遊戲 1 單次遊玩所需遊戲代幣數量 |
| `/dice-game-2` | 所有成員 | 小遊戲 | 使用遊戲代幣玩骰子遊戲 2（順子／三條計分），獲得高額貨幣獎勵 |
| `/dice-game-2-config` | 管理員 | 小遊戲 | 設定或查看骰子遊戲 2 單次遊玩所需遊戲代幣數量 |
| `/user-panel` | 所有成員 | 面板 | 顯示個人面板（貨幣餘額、遊戲代幣、代幣流水入口） |
| `/admin-panel` | 管理員 | 面板 | 顯示管理面板，用按鈕與表單管理餘額、代幣與遊戲設定 |

---

## `/balance` – 查看個人貨幣餘額

- **權限**：所有成員
- **用途**：顯示呼叫者在當前伺服器的貨幣餘額，以及該伺服器設定的貨幣名稱與圖示。
- **參數**：無

**使用範例**

```text
/balance
```

**典型回應**

```text
💰 你的餘額
金幣：1,000
```

> 備註：若該伺服器尚未設定貨幣，系統會回傳錯誤訊息提示管理員先執行 `/currency-config`。

---

## `/currency-config` – 設定／查看伺服器貨幣

- **權限**：需具備 Administrator 權限
- **用途**：設定或檢視伺服器使用的貨幣名稱與圖示。

### 參數

| 名稱 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `name` | string | 否 | 貨幣名稱，例如 `金幣`、`鑽石` |
| `icon` | string | 否 | 貨幣圖示，可為標準 emoji (`💰`) 或自訂表情（例如 `<:coin:1234567890>`） |

> 若兩個參數都省略，指令會顯示目前伺服器的貨幣設定。

### 使用範例

```text
/currency-config name:金幣 icon:💰
```

**典型回應**

```text
✅ Currency configuration updated!
Name: 金幣
Icon: 💰
```

---

## `/adjust-balance` – 調整成員貨幣餘額

- **權限**：需具備 Administrator 權限
- **用途**：以三種模式調整成員的貨幣餘額：
  - `add`：增加餘額
  - `deduct`：扣除餘額
  - `adjust`：直接將餘額設定為指定數值

### 參數

| 名稱 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `mode` | string (choice) | 是 | 調整模式：`add`、`deduct` 或 `adjust` |
| `member` | user | 是 | 要調整的目標成員 |
| `amount` | integer | 是 | - `add` / `deduct` 模式：調整金額（需為正整數）<br>- `adjust` 模式：目標餘額（需為 0 或正整數） |

### 使用範例

- 增加餘額：

  ```text
  /adjust-balance mode:add member:@成員 amount:500
  ```

- 扣除餘額：

  ```text
  /adjust-balance mode:deduct member:@成員 amount:200
  ```

- 設定為指定餘額：

  ```text
  /adjust-balance mode:adjust member:@成員 amount:1000
  ```

**典型回應（add 模式）**

```text
Added 💰 500 金幣 to @成員
New balance: 💰 1,000 金幣
```

> 限制條件：餘額不得變成負數，且單次調整金額不得超過系統定義的最大調整上限；若違反會回傳錯誤訊息。

---

## `/game-token-adjust` – 調整成員遊戲代幣

- **權限**：需具備 Administrator 權限
- **用途**：為成員增加或減少遊戲代幣。

### 參數

| 名稱 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `member` | user | 是 | 要調整的目標成員 |
| `amount` | integer | 是 | 調整數量，正數代表增加，負數代表扣除，不能為 0 |

### 使用範例

- 增加遊戲代幣：

  ```text
  /game-token-adjust member:@成員 amount:10
  ```

- 扣除遊戲代幣：

  ```text
  /game-token-adjust member:@成員 amount:-5
  ```

**典型回應**

```text
增加 10 遊戲代幣給 @成員
新餘額：🎮 25
```

> 限制條件：代幣餘額不得變成負數，否則會回傳錯誤訊息。

---

## `/dice-game-1` – 骰子遊戲 1

- **權限**：所有成員
- **用途**：消耗一定數量的遊戲代幣，擲 5 顆骰子，依點數總和換算伺服器貨幣獎勵。
- **遊戲規則概要**：
  - 每局擲 5 顆骰子（點數 1–6）。
  - 每顆骰子的獎勵為：`點數 × 250,000`，合計為本局總獎勵。
  - 獎勵會自動加入玩家在該伺服器的貨幣帳戶。
  - 每次遊戲消耗的代幣數量由 `/dice-game-1-config` 設定。

### 參數

- 無（代幣消耗依伺服器設定）

### 使用範例

```text
/dice-game-1
```

**典型回應（示意）**

```text
**Dice Game Results**
Rolls: 🎲 2, 4, 6, 3, 5
Total reward: 💰 5,000,000 金幣
Previous balance: 💰 10,000,000
New balance: 💰 15,000,000
```

> 若玩家遊戲代幣不足，系統會提示所需代幣與目前代幣餘額。

---

## `/dice-game-1-config` – 設定骰子遊戲 1 代幣消耗

- **權限**：需具備 Administrator 權限
- **用途**：設定或查看進行 `/dice-game-1` 時，每局需要消耗的遊戲代幣數量。

### 參數

| 名稱 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `token-cost` | integer | 否 | 每局遊戲需要的遊戲代幣數量，省略時僅顯示目前設定 |

### 使用範例

- 查看目前設定：

  ```text
  /dice-game-1-config
  ```

- 將每局消耗設定為 3 代幣：

  ```text
  /dice-game-1-config token-cost:3
  ```

**典型回應（設定成功）**

```text
Dice game configuration updated!
Tokens required per play: 3
```

---

## `/dice-game-2` – 骰子遊戲 2

- **權限**：所有成員
- **用途**：消耗一定數量的遊戲代幣，擲 15 顆骰子，依「順子」與「三條」組合計分，可獲得高額貨幣獎勵。

> 詳細加權計分邏輯實作於 `DiceGame2Service` 中，這裡僅描述行為層面：玩家消耗代幣，依骰子結果獲得一筆貨幣獎勵，並記錄在貨幣帳戶中。

### 參數

- 無（代幣消耗依伺服器設定）

### 使用範例

```text
/dice-game-2
```

**典型回應（示意）**

```text
**Dice Game 2 Results**
Rolls: 🎲 15 顆骰子的結果…
Score: 4 straights, 2 triples
Total reward: 💰 50,000,000 金幣
Previous balance: 💰 100,000,000
New balance: 💰 150,000,000
```

> 若玩家遊戲代幣不足，同樣會顯示所需代幣與目前代幣餘額。

---

## `/dice-game-2-config` – 設定骰子遊戲 2 代幣消耗

- **權限**：需具備 Administrator 權限
- **用途**：設定或查看進行 `/dice-game-2` 時，每局需要消耗的遊戲代幣數量。

### 參數

| 名稱 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `token-cost` | integer | 否 | 每局遊戲需要的遊戲代幣數量，省略時僅顯示目前設定 |

### 使用範例

```text
/dice-game-2-config token-cost:5
```

**典型回應**

```text
Dice game 2 configuration updated!
Tokens required per play: 5
```

---

## `/user-panel` – 個人面板

- **權限**：所有成員
- **用途**：以 Embed 形式顯示成員在該伺服器的：
  - 貨幣餘額與貨幣名稱／圖示
  - 遊戲代幣餘額
  - 查看遊戲代幣流水的按鈕

### 參數

- 無

### 使用範例

```text
/user-panel
```

**典型回應**

- 一個僅自己可見（ephemeral）的 Embed，包含：
  - 「貨幣」欄位：顯示目前餘額與貨幣名稱／圖示
  - 「遊戲代幣」欄位：顯示目前代幣餘額
  - 「📜 查看遊戲代幣流水」按鈕

點擊「📜 查看遊戲代幣流水」後，Bot 會顯示分頁式的交易紀錄，每一筆紀錄包含時間與說明，例如：

```text
2025-12-01 12:34 +10 🎮 (骰子遊戲 1 獎勵)
2025-12-01 12:30 -3 🎮 (骰子遊戲 1 消耗)
...
```

---

## `/admin-panel` – 管理面板

- **權限**：需具備 Administrator 權限
- **用途**：提供圖形化管理介面，透過按鈕、下拉選單與 Modal 來：
  - 調整成員貨幣餘額
  - 調整成員遊戲代幣餘額
  - 調整骰子遊戲 1 / 2 的代幣消耗設定

### 參數

- 無

### 使用流程概要

1. 管理員輸入：

   ```text
   /admin-panel
   ```

2. Bot 會回覆一個僅管理員可見的管理面板 Embed，並附上三個主要按鈕：
   - 「💰 使用者餘額管理」
   - 「🎮 遊戲代幣管理」
   - 「🎲 遊戲設定管理」

3. 點擊「使用者餘額管理」會彈出表單（Modal），可輸入：
   - 使用者 ID
   - 調整模式（增加／扣除／設定為指定值）
   - 金額

4. 點擊「遊戲代幣管理」會彈出表單，可輸入：
   - 使用者 ID
   - 調整金額（可為正負）

5. 點擊「遊戲設定管理」會先顯示選單讓你選擇遊戲種類（骰子遊戲 1 或 2），再顯示表單輸入新的代幣消耗數量。

> `/admin-panel` 是對 `/adjust-balance`、`/game-token-adjust`、`/dice-game-1-config`、`/dice-game-2-config` 的圖形化封裝，方便在手機或 GUI 情境下操作。

