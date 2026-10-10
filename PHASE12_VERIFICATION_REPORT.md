# RakshaAI — Phase 12 Real-Device Verification & OWASP Security Assessment Report

## 1. Inspection and Baseline Status

- **Project Path**: `E:/AndroidProject/RakshaAI`
- **Gradle Build Target**: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 24`
- **Baseline Build Verification**: Task `:app:assembleDebug` completed successfully.
- **Baseline Automated Test Suite**: Task `:app:testDebugUnitTest` executed **40 unit tests** with **100% pass rate** (0 skipped, 0 failed).

---

## 2. Privacy Controls & Monitoring Lifecycle Matrix

| Control | Setting Key | Default | Runtime Behavior & Lifecycle | Permission / Dependency | Effective Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Website Monitoring** | `websiteMonitoringEnabled` | `true` | Online reputation requests via `URLhausProvider` + `ThreatIntelligenceStore` local lookup. Switches to local mode when offline (`WAITING_FOR_INTERNET`). | Internet capability (`NET_CAPABILITY_INTERNET`) | **VERIFIED** |
| **APK & App Installation Monitoring** | `apkMonitoringEnabled` | `true` | Intercepts `DOWNLOAD_COMPLETE` broadcasts & package additions (`PACKAGE_ADDED`, `PACKAGE_REPLACED`). Computes SHA-256 and evaluates double-extension heuristics. Offers SAF manual scan. | None required (System Receiver + SAF) | **VERIFIED** |
| **Notification Scam Detection** | `notificationMonitoringEnabled` | `false` | Analyzes incoming notifications on-device using ML TF-IDF + Linear SVM classifier. Includes in-memory 60s window + Room DB cross-restart deduplication (`countRecentScansWithContent`). | Notification Access (`enabled_notification_listeners`) | **VERIFIED** |
| **Network Connection Monitoring** | `networkMonitoringEnabled` | `false` | Launches `RakshaVpnService` foreground service with `startForeground` notification and local TUN interface (`10.10.0.1/24`) for metadata inspection. | `VpnService.prepare()` consent | **VERIFIED** |
| **Automatic Threat Database Updates** | `automaticThreatUpdatesEnabled` | `true` | Schedules unique WorkManager job (`ThreatUpdateWorker`) under network constraints. Supports manual "Refresh Feed Now" action. | Network connection + WorkManager | **VERIFIED** |
| **Security Alerts** | `securityAlertsEnabled` | `true` | Triggers high-priority system notification alerts for high-risk threats. | `POST_NOTIFICATIONS` (Android 13+) | **VERIFIED** |

---

## 3. VPN & Online Threat Intelligence Audit Findings

1. **VPN Architecture Audit (`RakshaVpnService.kt` & `VpnManager.kt`)**:
   - The VPN implementation creates a local TUN interface (`10.10.0.1/24`) to inspect packet metadata without acting as an outbound user-space NAT proxy.
   - **Remediation Applied**: Fixed foreground service lifecycle by registering notification channel `rakshaai_vpn_channel` and invoking `startForeground(...)` with `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 34+. This prevents `ForegroundServiceDidNotStartInTimeException` on Android 8.0 through Android 15.
   - **UI Disclosure**: Accurately labeled in UI as "Local security VPN for connection metadata analysis".

2. **Threat Intelligence Integration (`URLhausProvider` & `CompositeThreatIntelProvider`)**:
   - Requests are issued over secure HTTPS (`https://urlhaus-api.abuse.ch/v1/url/`) with 5-second connection timeouts.
   - Fallback logic checks `ThreatIntelligenceStore` (PhishTank local database) first. If offline, network calls are skipped cleanly without crashing.

---

## 4. OWASP Mobile Application Security Assessment (MASVS)

| OWASP MASVS Category | Control ID | Status | Audit Findings & Verification |
| :--- | :--- | :--- | :--- |
| **MASVS-STORAGE** | V1.1 / V1.2 | **VERIFIED** | Local Room Database (`scan_database.db`) uses parameterized DAO queries. Temporary APK scan cache files (`apk_scan_*.apk`) are cleaned up immediately in `finally` blocks. Preferences persist securely in private `SharedPreferences`. |
| **MASVS-CRYPTO** | V2.1 | **VERIFIED** | SHA-256 message digests (`MessageDigest.getInstance("SHA-256")`) calculated locally via standard Java Security APIs. No weak cryptographic algorithms used. |
| **MASVS-NETWORK** | V3.1 / V3.2 | **VERIFIED** | Threat intelligence lookups use HTTPS endpoints with 5s timeouts. Transport Layer Security (TLS) enforced. No plaintext HTTP credentials transmitted. |
| **MASVS-PLATFORM** | V4.1 / V4.2 | **VERIFIED** | AndroidManifest exported attributes declared explicitly. Receiver broadcasts validate intents safely. `FileProvider` configured with `exported="false"` and scoped paths (`@xml/file_paths`). |
| **MASVS-CODE** | V7.1 / V7.2 | **VERIFIED** | Zero hardcoded API keys in application sources. Input sanitization applied before ML inference and regex evaluation. Logcat logging sanitized against sensitive PII leakage. |

---

## 5. Summary of Remediation Fixes Applied in Phase 12

1. **`RakshaVpnService.kt` Foreground Service Fix**:
   - Added notification channel creation and `startForeground(2025, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` call. Resolved background startup crashing risks on Android 8.0–15.
2. **`UrlAnalyzer.kt` & `LocalDetectionEngine.kt` Real-Time Online Reputation Wiring**:
   - Integrated `analyzeAsync` with `CompositeThreatIntelProvider` so online malware/phishing threats (URLhaus) are checked in real-time when online monitoring is enabled.
3. **`UrlRuleEngine.kt` Provider Disambiguation**:
   - Formatted threat signals to dynamically report whether a threat match originated from `URLhaus` or `PhishTank`.
4. **Unit Test Suite Expansion**:
   - Added `SecuritySettingsAndMonitoringTest` covering `CompositeThreatIntelProvider`, `URLhausProvider` offline fallback, `UrlAnalyzer.analyzeAsync`, and SHA-256 hashing. Expanded test suite from 29 to 40 passing tests.

---

## 6. Android Platform Limitations

1. **Browser Pre-Installation Download Interception**: Standard Android permission models prevent normal applications from intercepting all third-party browser downloads prior to installation. Detection is provided for supported `DownloadManager` events, package additions, and SAF manual file scans.
2. **Third-Party APK Sideloading**: Unverified third-party APKs are evaluated for risk using SHA-256 hashes, double extension heuristics (`.apk.exe`), and package signatures rather than automatically labeling all non-Play Store apps as malware.
3. **Notification Access Permission**: Notification scanning requires explicit user authorization in Android system settings.
