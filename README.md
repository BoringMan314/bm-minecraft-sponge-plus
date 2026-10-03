# [B.M] Minecraft 海綿 PLUS

[![Paper](https://img.shields.io/badge/Paper-26.3-2D2D2D)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![GitHub](https://img.shields.io/badge/GitHub-bm--minecraft--sponge--plus-181717?logo=github)](https://github.com/BoringMan314/bm-minecraft-sponge-plus)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

適用於 **Minecraft Paper 26.3** 的插件：擴大原版海綿的連通式吸水半徑，並保留濕海綿與烤乾機制。

*适用于 **Minecraft Paper 26.3** 的插件：扩大原版海绵的连通式吸水半径，并保留湿海绵与烘干机制。*<br>
*Minecraft Paper 26.3 向け：スポンジの連続した水の吸収半径を拡大し、濡れたスポンジの挙動を維持します。*<br>
*A **Minecraft Paper 26.3** plugin that extends vanilla connected-water sponge absorption while retaining wet-sponge behavior.*

> **說明**：僅支援 Paper 26.3 與 Java 25。

---

## 目錄

- [功能](#功能)
- [系統需求](#系統需求)
- [安裝方式](#安裝方式)
- [指令與權限](#指令與權限)
- [設定檔](#設定檔)
- [本機建置](#本機建置)
- [專案結構](#專案結構)
- [版本與多語系](#版本與多語系)
- [資料與隱私說明](#資料與隱私說明)
- [授權](#授權)
- [問題與建議](#問題與建議)

---

## 功能

- 放置海綿後，依連通水路徑擴大吸水搜尋半徑。
- 預設半徑 `10`，最大包圍範圍為 `21×21×21`；只處理連通水域。
- 吸到水後正常轉為濕海綿；烤乾速度與方式維持原版。
- 支援開關、重載、狀態與半徑設定。

---

## 系統需求

- **Paper 26.3** 伺服器。
- **Java 25**。

---

## 安裝方式

1. 從 [`dist/`](dist/) 選擇所需語系 JAR。
2. 將 JAR 放入 Paper 伺服器的 `plugins/` 資料夾。
3. 啟動伺服器後，設定檔建立於 `plugins/bm-minecraft-sponge-plus/`。

> 請勿同時安裝多個語系 JAR；它們是同一插件的不同預設語言版本。

---

## 指令與權限

| 指令 | 說明 | 權限 |
| --- | --- | --- |
| `/bm-minecraft-sponge-plus 0/1` | 開關功能 | `.admin` |
| `/bm-minecraft-sponge-plus reload` | 重載設定 | `.admin` |
| `/bm-minecraft-sponge-plus info` | 顯示資訊 | `.admin` |
| `/bm-minecraft-sponge-plus status` | 顯示狀態 | `.admin` |
| `/bm-minecraft-sponge-plus set <半徑>` | 設定半徑 | `.admin` |

`bm-minecraft-sponge-plus.use` 預設所有玩家可用；`.admin` 預設 OP 可用。

---

## 設定檔

```yml
enabled: true
absorption-radius: 10
admin-require-op: true
```

半徑 `10` 代表相對海綿各軸最多 10 格，最大包圍範圍為 `21×21×21`；只吸收與海綿連通的水。

---

## 本機建置

執行 `build.bat`；預設會在結束時暫停。自動化環境使用：

```bat
build.bat --no-pause
```

會在 `dist/` 產生 `zh_TW`、`zh_CN`、`ja_JP`、`en_US` JAR。

---

## 專案結構

```text
src/main/java/bm.minecraft.sponge.plus/
src/main/resources/lang/
src/main/resources/config.yml
```

---

## 版本與多語系

版本為 `26.3_0.0.1`；建置時以 `active-language.yml` 選擇四種語系之一。

---

## 資料與隱私說明

本插件不儲存玩家資料或外部資料。

---

## 授權

本專案以 [MIT License](LICENSE) 授權。

---

## 問題與建議

歡迎透過 [GitHub Issues](https://github.com/BoringMan314/bm-minecraft-sponge-plus/issues) 回報錯誤或提出改善建議。回報時請一併提供 Paper 版本、Java 版本、設定檔與完整錯誤日誌。
