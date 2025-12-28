# Research: Spotless Code Format

**Feature**: Spotless Code Format
**Date**: 2025-12-28
**Phase**: Phase 0 - Research & Decision Making

---

## Executive Summary

本研究確定使用 **Spotless Maven Plugin 3.1.0** 作為代碼格式化解決方案，配合 **Google Java Format 1.15.0+**（預設）。此組合與現有專案（Java 17 + Maven + Dagger 2 + JaCoCo）完全兼容，無需額外 JVM 參數，且自動排除 Dagger 生成的代碼。

---

## Technology Decisions

### Decision 1: Spotless Maven Plugin Version

| 選項 | 版本 | 決定 |
|------|------|------|
| **Spotless Maven Plugin** | **3.1.0** | **ADOPTED** |

**Rationale**:
- 最新穩定版本（2024年11月發布）
- 與 Java 17 完全兼容，無需 `--add-exports` 參數
- 自動排除 `target/` 目錄中的生成代碼（2.41.0+ 修復）
- 活躍維護，社群反饋良好

**Alternatives Considered**:
1. **Checkstyle + Maven Compiler Plugin**：配置複雜，需要手動維護規則集
2. **Google Java Format Maven Plugin**：僅提供格式化，無檢查功能
3. **Spotless 2.x**：舊版本，Java 17 兼容性需額外配置

**References**:
- [Maven Central - Spotless Maven Plugin](https://central.sonatype.com/artifact/com.diffplug.spotless/spotless-maven-plugin)
- [Spotless CHANGES.md](https://github.com/diffplug/spotless/blob/main/CHANGES.md)

---

### Decision 2: Java Format Standard

| 選項 | 標準 | 決定 |
|------|------|------|
| **Code Style** | **Google Java Format** | **ADOPTED** |

**Rationale**:
- Java 生態系中最廣泛接受的風格指南
- 與 Spotless 深度整合，配置簡單
- 自動處理 import 排序和未使用 import 移除
- IntelliJ IDEA 和 VS Code 均有原生支援

**Alternatives Considered**:
1. **Spring Java Format**：僅適用 Spring 生態，過於具體
2. **Eclipse JDT**：配置複雜，需要手動調整
3. **自訂規則**：維護成本高

**Configuration**:
```xml
<googleJavaFormat>
    <style>GOOGLE</style>
    <reflowLongStrings>true</reflowLongStrings>
</googleJavaFormat>
```

**References**:
- [Google Java Format GitHub](https://github.com/google/google-java-format)
- [Spotless - Google Java Format](https://github.com/diffplug/spotless/tree/main/plugin-maven#google-java-format)

---

### Decision 3: Build Integration Strategy

| 選項 | 策略 | 決定 |
|------|------|------|
| **Build Phase** | **verify phase with check goal** | **ADOPTED** |

**Rationale**:
- `verify` 階段在 `test` 之後執行，確保測試通過後才檢查格式
- `check` goal 在 CI/CD 中失敗建置，防止不合規代碼合併
- 開發人員可手動執行 `mvn spotless:apply` 修正格式
- 不影響現有 JaCoCo 覆蓋率檢查（同在 verify 階段）

**Execution Flow**:
```text
compile → test → jacoco:check → spotless:check → verify → install
```

**Alternatives Considered**:
1. **compile phase with apply**：自動修改代碼，可能引入意外變更
2. **validate phase**：過早執行，編譯失敗時浪費時間
3. **手動執行**：無法強制執行，依賴開發人員記憶

**References**:
- [Spotless Maven Plugin - Execution](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md)
- [Maven Lifecycle - verify phase](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)

---

### Decision 4: Generated Code Exclusion

| 選項 | 策略 | 決定 |
|------|------|------|
| **Exclusion** | **Automatic (target/ directory)** | **ADOPTED** |

**Rationale**:
- Spotless 2.41.0+ 自動排除 `target/` 目錄
- Dagger 生成代碼位於 `target/generated-sources/annotations/`
- 無需手動配置 `<excludes>` 規則
- 簡化維護，降低配置錯誤風險

**Dagger Generated Locations**:
```
target/generated-sources/annotations/
├── ltdjms/discord/currency/dagger/
│   ├── AppComponent.java
│   ├── DaggerAppComponent.java
│   └── DaggerAppComponent$*.java
```

**Alternatives Considered**:
1. **手動排除特定模式**：維護成本高，容易遺漏
2. **使用 license header 識別**：生成代碼無法可靠識別
3. **不排除**：格式化生成代碼會在下一次生成時被覆蓋

**References**:
- [GitHub Issue #1914 - Generated sources formatting](https://github.com/diffplug/spotless/issues/1914)

---

### Decision 5: IDE Integration

| 選項 | 工具 | 決定 |
|------|------|------|
| **IntelliJ IDEA** | **Spotless Applier Plugin** | **RECOMMENDED** |
| **VS Code** | **Maven Tasks (spotless:apply)** | **RECOMMENDED** |

**Rationale**:

**IntelliJ IDEA**:
- 插件提供 "Format Current File" 和 "Format Project" 功能
- 快捷鍵支援，無需切換到終端機
- 與 Spotless 配置同步，確保一致性

**VS Code**:
- 使用 `.vscode/tasks.json` 配置 Maven goals
- 支援 format on save（需額外擴充功能）
- 跨平台一致性

**Alternative Considered**:
- **手動執行 Maven goals**：可用但效率較低

**References**:
- [Spotless Applier Plugin - JetBrains Marketplace](https://plugins.jetbrains.com/plugin/22455-spotless-applier)
- [VS Code Java Formatting Documentation](https://code.visualstudio.com/docs/java/java-linting)

---

## Maven Configuration

### Recommended pom.xml Configuration

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

---

## Makefile Integration

### Recommended Makefile Additions

```makefile
.PHONY: format format-check

# 格式化代碼
format:
	mvn spotless:apply

# 檢查格式
format-check:
	mvn spotless:check

# 更新現有 test 目標（可選）
test: format-check
	mvn test
```

---

## Common Issues & Mitigations

### Issue 1: Merge Conflicts Due to Formatting

**Symptoms**: Git merge conflicts caused by formatting differences

**Solution**:
- 在 CI/CD 中強制執行 `spotless:check`
- 使用 Git pre-commit hook（可選）
- 文檔化格式檢查流程

### Issue 2: Tests Fail After Formatting

**Symptoms**: Unit/integration tests fail after code formatting

**Solution**:
- 測試應基於行為而非格式
- 在提交前執行 `mvn spotless:apply test`
- CI/CD 中順序執行：compile → test → spotless:check

### Issue 3: Format Inconsistency Between IDE and CLI

**Symptoms**: IDE formats differently than Spotless

**Solution**:
- IntelliJ IDEA: 安裝 Spotless Applier Plugin
- VS Code: 使用 Maven tasks 或 spotless-gradle extension
- 文檔化 IDE 配置步驟

---

## Implementation Phases

### Phase 1: Initial Setup
1. 添加 Spotless Maven Plugin 到 `pom.xml`
2. 執行 `mvn spotless:apply` 格式化現有代碼
3. 驗證所有測試仍通過 (`mvn test`)

### Phase 2: Build Integration
1. 確認 `spotless:check` 在 `verify` 階段執行
2. 測試完整建置流程 (`mvn verify`)
3. 更新 `Makefile` 添加 `format` 和 `format-check` 目標

### Phase 3: Documentation
1. 更新 `docs/development/testing.md` 添加格式檢查指令
2. 建立 IDE 配置指南（IntelliJ IDEA / VS Code）
3. 建立 Git pre-commit hook 文檔（可選）

### Phase 4: CI/CD Integration
1. 確認 CI/CD pipeline 中執行 `mvn verify`
2. 驗證格式違規會導致建置失敗
3. 文檔化 CI/CD 格式檢查流程

---

## Success Criteria Alignment

| Spec Criteria | Implementation Approach |
|---------------|-------------------------|
| **SC-001**: 零格式違規 | `mvn spotless:check` 驗證 |
| **SC-002**: 測試仍通過 | 格式化後執行完整測試套件 |
| **SC-003**: 建置流程整合 | verify 階段執行 check |
| **SC-004**: 單命令修復 | `mvn spotless:apply` |

---

## Open Questions

**無** - 所有技術決策已完成。

---

## References

### Official Documentation
- [Spotless GitHub Repository](https://github.com/diffplug/spotless)
- [Spotless Maven Plugin README](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md)

### Version Information
- [Maven Central - Spotless Maven Plugin](https://central.sonatype.com/artifact/com.diffplug.spotless/spotless-maven-plugin)

### Tutorials & Best Practices
- [Baeldung - Maven Spotless Plugin for Java](https://www.baeldung.com/java-maven-spotless-plugin)
- [Apache ShardingSphere - Spotless 實戰](https://shardingsphere.apache.org/blog/cn/material/spotless/)

### IDE Integration
- [Spotless Applier Plugin - JetBrains Marketplace](https://plugins.jetbrains.com/plugin/22455-spotless-applier)

### Community Discussions
- [Stack Overflow - Spotless Questions](https://stackoverflow.com/questions/tagged/spotless)
- [GitHub Issues - Spotless](https://github.com/diffplug/spotless/issues)
