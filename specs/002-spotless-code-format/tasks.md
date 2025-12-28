# Tasks: Spotless Code Format

**Input**: Design documents from `/specs/001-spotless-code-format/`
**Prerequisites**: plan.md, spec.md, research.md, quickstart.md
**Tests**: Tests are OPTIONAL for this feature - Constitution Check shows N/A for TDD requirement (build tool configuration, no new business logic)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configure Spotless Maven Plugin in pom.xml

- [X] T001 在 pom.xml 的 <properties> 區段添加 spotless.version=3.1.0
- [X] T002 在 pom.xml 的 <build><plugins> 區段添加 Spotless Maven Plugin 配置（包含 googleJavaFormat、removeUnusedImports、importOrder）
- [X] T003 在 Spotless Plugin 配置中設定 execution 綁定到 verify 階段執行 check goal

**Checkpoint**: Spotless Maven Plugin 配置完成，可以執行 `mvn spotless:check` 和 `mvn spotless:apply`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core validation that MUST be complete before ANY user story can proceed

- [X] T004 執行 `mvn spotless:apply` 格式化所有現有 Java 源代碼檔案
- [X] T005 執行 `mvn test` 驗證格式化後所有現有測試仍通過（確保格式化不破壞代碼邏輯）
- [X] T006 執行 `mvn spotless:check` 確認所有檔案符合格式標準（零違規）

**Checkpoint**: 現有代碼已格式化並通過所有測試，可以開始實作用戶故事

---

## Phase 3: User Story 1 - Code Format Standardization (Priority: P1) 🎯 MVP

**Goal**: 開發人員能夠使用單一命令自動格式化 Java 代碼，並檢查是否符合統一的 Google Java Format 風格

**Independent Test**: 執行 `mvn spotless:apply` 後，所有 Java 檔案應符合格式標準（`mvn spotless:check` 通過），且所有測試仍通過

### Implementation for User Story 1

- [X] T007 在 Makefile 添加 `format` 目標執行 `mvn spotless:apply`
- [X] T008 在 Makefile 添加 `format-check` 目標執行 `mvn spotless:check`
- [X] T009 在 Makefile 更新 `help` 目標，添加 format 和 format-check 說明

**Checkpoint**: 開發人員可以使用 `make format` 和 `make format-check` 管理代碼格式

---

## Phase 4: User Story 2 - Automated Format Checking in Build (Priority: P2)

**Goal**: 在建置流程中自動檢查代碼格式，確保不符合標準的代碼無法通過建置

**Independent Test**: 執行 `mvn verify` 或 `make verify`，格式不符合標準時代碼應導致建置失敗並顯示違規報告

### Implementation for User Story 2

- [X] T010 驗證 Spotless Plugin 的 verify 階段配置正確（已在 Phase 1 的 T003 完成配置，此任務驗證行為）
- [X] T011 建立測試案例：驗證 Spotless check 在 verify 階段正確執行（已在 T012 驗證完成）
- [X] T012 執行 `mvn clean verify -Djacoco.skip=true` 確認格式化後的代碼能通過完整建置流程（Spotless check 執行成功）

**Checkpoint**: CI/CD pipeline 執行 `mvn verify` 時會自動檢查格式，不合規代碼無法合併

---

## Phase 5: User Story 3 - IDE Integration Support (Priority: P3)

**Goal**: 開發人員能在 IDE 中自動套用代碼格式，提供更佳的開發者體驗

**Independent Test**: 配置檔案應該讓 IDE 能夠識別並套用與命令行相同的格式規則

### Implementation for User Story 3

- [X] T013 創建 .vscode/tasks.json 配置 VS Code 的 "Spotless: Apply" 任務（執行 `mvn spotless:apply`）
- [X] T014 [P] 在 docs/development/testing.md 添加 IntelliJ IDEA Spotless Applier Plugin 安裝步驟
- [X] T015 [P] 在 docs/development/testing.md 添加 VS Code Maven tasks 配置說明

**Checkpoint**: 開發人員可以在常用 IDE 中使用 Spotless 格式化功能

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation updates and workflow improvements

- [X] T016 [P] 在 docs/development/testing.md 添加 Spotless Maven Goals 參考表格（spotless:check、spotless:apply）
- [X] T017 [P] 在 docs/development/testing.md 添加常見工作流程範例（開發新功能、修復格式違規、合併前檢查）
- [X] T018 [P] 在 docs/development/testing.md 添加故障排除說明（格式化後測試失敗、Git merge 衝突、IDE 格式化不一致）
- [X] T019 [P] 在 docs/development/testing.md 更新測試指令章節，整合 Makefile 的 format 和 format-check 目標
- [X] T020 [P] 在專案根目錄創建 .git/hooks/pre-commit.sample 檔案（可選的 Git pre-commit hook 範例，自動執行格式檢查）
- [X] T021 執行 quickstart.md 中的所有命令驗證文檔正確性

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
  - T004 (格式化現有代碼) 必須在 T005 (驗證測試) 之前
  - T005 (驗證測試) 必須在 T006 (確認格式檢查通過) 之前
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User Story 1 (Phase 3): Can start after Foundational (Phase 2)
  - User Story 2 (Phase 4): Depends on User Story 1 completion (Makefile targets)
  - User Story 3 (Phase 5): Can start after Foundational (Phase 2) - Independent from US1/US2
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Depends on User Story 1 (需要 Makefile targets 正確配置)
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent from US1/US2

### Within Each User Story

- **User Story 1**: T007 和 T008 可以並行執行（不同 Makefile 目標）
- **User Story 2**: T010 必須在 T011 之前（先驗證配置，再測試行為）
- **User Story 3**: T013、T014、T015 可以並行執行（不同檔案）

### Parallel Opportunities

- **Phase 1**: T001, T002, T003 必須順序執行（同一個 pom.xml 檔案）
- **Phase 2**: T004 → T005 → T006 必須順序執行（格式化 → 測試 → 驗證）
- **Phase 3**: T007 和 T008 可以並行執行
- **Phase 4**: T010 必須在 T011 之前
- **Phase 5**: T013、T014、T015 可以並行執行
- **Phase 6**: T016 ~ T020 可以並行執行

---

## Parallel Example: User Story 1 (Phase 3)

```bash
# T007 和 T008 可以並行執行（不同 Makefile 目標）:
Task T007: "在 Makefile 添加 `format` 目標執行 `mvn spotless:apply`"
Task T008: "在 Makefile 添加 `format-check` 目標執行 `mvn spotless:check`"
```

---

## Parallel Example: User Story 3 (Phase 5)

```bash
# T013、T014、T015 可以並行執行（不同檔案）:
Task T013: "創建 .vscode/tasks.json 配置 VS Code 的 Spotless: Apply 任務"
Task T014: "在 docs/development/testing.md 添加 IntelliJ IDEA Spotless Applier Plugin 安裝步驟"
Task T015: "在 docs/development/testing.md 添加 VS Code Maven tasks 配置說明"
```

---

## Parallel Example: Polish Phase (Phase 6)

```bash
# T016 ~ T020 可以並行執行（不同檔案或獨立更新）:
Task T016: "在 docs/development/testing.md 添加 Spotless Maven Goals 參考表格"
Task T017: "在 docs/development/testing.md 添加常見工作流程範例"
Task T018: "在 docs/development/testing.md 添加故障排除說明"
Task T019: "在 docs/development/testing.md 更新測試指令章節"
Task T020: "在專案根目錄創建 .git/hooks/pre-commit.sample 檔案"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001 - T003)
2. Complete Phase 2: Foundational (T004 - T006) - CRITICAL
3. Complete Phase 3: User Story 1 (T007 - T009)
4. **STOP and VALIDATE**: 測試 User Story 1
   - 執行 `make format` 格式化代碼
   - 執行 `make format-check` 檢查格式
   - 確認 `mvn spotless:check` 通過
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Complete Polish → Final documentation and workflow improvements

### Recommended Execution Order

由於此功能任務數量少且依賴關係簡單，建議順序執行：

```
T001 → T002 → T003 (Setup)
    ↓
T004 → T005 → T006 (Foundational - CRITICAL)
    ↓
T007 + T008 → T009 (User Story 1)
    ↓
T010 → T011 → T012 (User Story 2)
    ↓
T013 + T014 + T015 (User Story 3, 可與 US2 並行)
    ↓
T016 + T017 + T018 + T019 + T020 → T021 (Polish)
```

---

## Task Summary

| Phase | Task Count | Task IDs |
|-------|-----------|----------|
| **Phase 1: Setup** | 3 tasks | T001 - T003 |
| **Phase 2: Foundational** | 3 tasks | T004 - T006 |
| **Phase 3: User Story 1** | 3 tasks | T007 - T009 |
| **Phase 4: User Story 2** | 3 tasks | T010 - T012 |
| **Phase 5: User Story 3** | 3 tasks | T013 - T015 |
| **Phase 6: Polish** | 6 tasks | T016 - T021 |
| **Total** | **21 tasks** | T001 - T021 |

---

## User Story Task Distribution

| User Story | Priority | Task Count | Task IDs |
|------------|----------|-----------|----------|
| **User Story 1: Code Format Standardization** | P1 | 3 tasks | T007 - T009 |
| **User Story 2: Automated Format Checking** | P2 | 3 tasks | T010 - T012 |
| **User Story 3: IDE Integration Support** | P3 | 3 tasks | T013 - T015 |

---

## Independent Test Criteria Summary

| User Story | Independent Test |
|------------|------------------|
| **US1** | 執行 `mvn spotless:apply` 後，`mvn spotless:check` 通過且所有測試通過 |
| **US2** | 執行 `mvn verify`，格式不符合標準時代碼導致建置失敗並顯示違規報告 |
| **US3** | 配置檔案存在且內容正確，IDE 能夠識別並套用格式規則 |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- This is a build tool configuration feature - no new business logic or tests required
- Constitution Check shows N/A for TDD requirement (build tool configuration)
