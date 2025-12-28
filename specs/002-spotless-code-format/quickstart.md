# Quickstart Guide: Spotless Code Format

**Feature**: Spotless Code Format
**Branch**: `001-spotless-code-format`
**Last Updated**: 2025-12-28

---

## Overview

本指南說明如何在 LTDJMS 專案中使用 Spotless 進行代碼格式化。Spotless 是一個 Maven 外掛，自動檢查和修正 Java 代碼格式，確保代碼庫遵循統一的 Google Java Format 風格。

---

## Prerequisites

- **Java 17+**：專案已配置 Java 17
- **Maven 3.6+**：用於執行 Spotless goals
- **Git**：用於版本控制（可選 Git pre-commit hook）

---

## Quick Start

### 1. 檢查代碼格式

檢查所有 Java 檔案是否符合格式標準：

```bash
mvn spotless:check
```

**預期輸出**：
- 通過：`BUILD SUCCESS`
- 失敗：列出違規的檔案和差異

### 2. 自動修正格式

自動修正所有格式問題：

```bash
mvn spotless:apply
```

**重要**：修正後必須執行測試確保功能不變：

```bash
mvn test
```

### 3. 完整建置（包含格式檢查）

執行完整建置流程，包含格式檢查：

```bash
mvn verify
```

或使用 Makefile：

```bash
make verify
```

---

## Maven Goals Reference

| Goal | 說明 | 何時使用 |
|------|------|----------|
| `spotless:check` | 檢查格式，違規時失敗 | CI/CD、提交前 |
| `spotless:apply` | 自動修正格式問題 | 開發過程、修正違規 |

---

## Integration with Makefile

此功能新增以下 Makefile 目標：

```bash
# 格式化代碼
make format

# 檢查格式
make format-check

# 完整測試（包含格式檢查）
make test
```

---

## IDE Integration

### IntelliJ IDEA

#### 方法一：Spotless Applier Plugin（推薦）

1. **安裝插件**：
   - `File` → `Settings` → `Plugins` → 搜尋 "Spotless Applier"
   - 或訪問 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/22455-spotless-applier)

2. **使用**：
   - 右鍵檔案 → `Spotless: Format Current File`
   - 右鍵專案 → `Spotless: Format Project`

#### 方法二：外部工具

1. **建立外部工具**：
   ```
   File → Settings → Tools → External Tools → +
   Name: Spotless Apply
   Program: mvn
   Arguments: spotless:apply
   Working directory: $ProjectFileDir$
   ```

2. **設定快捷鍵**（可選）：
   ```
   Keymap → 搜尋 "Spotless Apply" → 設定快捷鍵（如 Cmd+Shift+F）
   ```

### VS Code

1. **建立任務**：`.vscode/tasks.json`
   ```json
   {
     "version": "2.0.0",
     "tasks": [
       {
         "label": "Spotless: Apply",
         "type": "shell",
         "command": "mvn spotless:apply",
         "problemMatcher": []
       }
     ]
   }
   ```

2. **執行**：
   - `Cmd+Shift+P` → "Tasks: Run Task" → "Spotless: Apply"

---

## Git Pre-commit Hook（可選）

自動在提交前檢查格式：

1. **建立 hook**：`.git/hooks/pre-commit`
   ```bash
   #!/bin/bash
   echo "Running Spotless format check..."
   mvn spotless:check
   if [ $? -ne 0 ]; then
       echo ""
       echo "❌ Code formatting check failed!"
       echo "Run 'mvn spotless:apply' to fix formatting issues."
       echo ""
       exit 1
   fi
   echo "✅ Spotless check passed!"
   ```

2. **使可執行**：
   ```bash
   chmod +x .git/hooks/pre-commit
   ```

---

## CI/CD Integration

格式檢查已整合到 `verify` 階段，CI/CD pipeline 應執行：

```bash
mvn verify
```

**預期行為**：
- 格式不符合標準：建置失敗
- 格式符合標準：建置成功

---

## Common Workflows

### Workflow 1: 開發新功能

```bash
# 1. 開發功能
# ... 編寫代碼 ...

# 2. 格式化代碼
mvn spotless:apply

# 3. 執行測試
mvn test

# 4. 提交
git add .
git commit -m "feat: new feature"
```

### Workflow 2: 修復格式違規

```bash
# 1. CI/CD 失敗，提示格式違規
# Running 'mvn spotless:check'

# 2. 修正格式
mvn spotless:apply

# 3. 確認測試仍通過
mvn test

# 4. 提交修正
git add .
git commit -m "fix: format code with spotless"
```

### Workflow 3: 合併前檢查

```bash
# 1. 拉取最新代碼
git pull origin main

# 2. 合併
git merge feature-branch

# 3. 完整建置檢查
mvn verify

# 4. 如果有格式問題
mvn spotless:apply
git add .
git commit -m "fix: format after merge"
```

---

## Troubleshooting

### Issue: 格式化後測試失敗

**原因**：格式化可能改變代碼結構

**解決方案**：
1. 確認測試基於行為而非格式
2. 檢查是否有測試依賴特定格式
3. 執行 `mvn clean test` 清除快取

### Issue: Git merge 衝突

**原因**：多人使用不同格式

**解決方案**：
1. Merge 前執行 `mvn spotless:apply`
2. 使用 Git pre-commit hook
3. CI/CD 中強制檢查格式

### Issue: IDE 格式化與 Spotless 不一致

**原因**：IDE 使用不同格式規則

**解決方案**：
- IntelliJ IDEA: 安裝 Spotless Applier Plugin
- VS Code: 使用 Maven tasks 而非內建格式化

---

## Configuration Details

### pom.xml 配置

```xml
<properties>
    <spotless.version>3.1.0</spotless.version>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>com.diffplug.spotless</groupId>
            <artifactId>spotless-maven-plugin</artifactId>
            <version>${spotless.version}</version>
            <configuration>
                <java>
                    <googleJavaFormat>
                        <style>GOOGLE</style>
                        <reflowLongStrings>true</reflowLongStrings>
                    </googleJavaFormat>
                    <removeUnusedImports />
                    <importOrder>
                        <order>java|javax,org,com,,#</order>
                    </importOrder>
                </java>
            </configuration>
            <executions>
                <execution>
                    <id>spotless-check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <phase>verify</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 格式規則說明

| 規則 | 說明 |
|------|------|
| **Google Java Format** | 使用 Google Java Style Guide |
| **Reflow Long Strings** | 自動重排過長的字串 |
| **Remove Unused Imports** | 移除未使用的 import 語句 |
| **Import Order** | 排序 import：java/javax → org → com → 無套件 |

### 排除規則

Spotless 自動排除以下目錄：
- `target/`：包含 Dagger 生成的代碼
- `target/generated-sources/`：所有生成代碼

無需手動配置排除規則。

---

## Related Documentation

- [研究報告](./research.md)：技術決策與版本選擇
- [測試策略](../../../docs/development/testing.md)：完整測試指令
- [專案 CLAUDE.md](../../../CLAUDE.md)：專案開發規範

---

## Support

- **GitHub Issues**: [diffplug/spotless](https://github.com/diffplug/spotless/issues)
- **Stack Overflow**: [spotless tag](https://stackoverflow.com/questions/tagged/spotless)
- **專案文檔**: `docs/development/testing.md`
