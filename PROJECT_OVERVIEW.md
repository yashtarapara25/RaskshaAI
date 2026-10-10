# 🛡️ RakshaAI — Phase 1 True Offline-First Cyber Assistant

> **Privacy-First On-Device Scam, Phishing & Malicious Content Detection Assistant**  
> *100% Offline Analysis for Messages, URLs, Screenshots & Photos*

---

## 📌 Executive Summary

**RakshaAI Phase 1** is a true **offline-first** cyber safety application for Android built according to the **PS-05 Product Development Guide**. It allows users to analyze suspicious:

1. **[ MESSAGE ]** — SMS, WhatsApp, emails, and multi-language text (Gujarati, Hindi, Hinglish, English).
2. **[ URL ]** — Web links, suspicious domains, IP hosts, and shorteners.
3. **[ SCREENSHOT ]** — Gallery screenshots with on-device ML Kit OCR text extraction.
4. **[ PHOTO ]** — Photos from gallery or live camera capture with on-device ML Kit OCR text extraction.

### 🔒 100% Offline & Private
- **Zero Cloud Uploads**: User content, messages, images, and scan history remain strictly on the device.
- **No Internet Dependency**: Operates without Firebase, OpenAI, Gemini, cloud APIs, or server requests.
- **On-Device ML Kit OCR**: Text extraction runs 100% locally via `com.google.mlkit:text-recognition`.

---

## 🎨 New Warm Neutral Light Design System

- **Primary Background**: `#F7F8F6` (Soft warm neutral off-white)
- **Secondary Surface**: `#FFFFFF` (Pure white cards/containers)
- **Primary Text**: `#17211B` (Dark charcoal)
- **Secondary Text**: `#66736B` (Muted slate)
- **Primary Brand Accent**: `#167C68` (Warm Security Teal)
- **Accent Container**: `#E6F4EF` (Soft light teal fill)
- **Subtle Borders**: `#E2E8E4`
- **Risk Status Colors**:
  - `SAFE` (0–29): `#17834B`
  - `SUSPICIOUS` (30–59): `#C77700`
  - `HIGH RISK` (60–79): `#D94A45`
  - `DANGEROUS` (80–100): `#B42318`

---

## 📁 Complete Directory & File Map

All project source files are located at `app/src/main/java/com/rakshaai/security/`:

```
E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/
│
├── MainActivity.kt                      # Main entry activity, Scaffold, and bottom navigation
│
├── data/                                # Data Layer
│   ├── model/
│   │   └── ScanModels.kt                # ContentType (Message, URL, Screenshot, Photo), RiskLevel, RiskCategory, ScanResult
│   └── repository/
│       └── ScanRepository.kt            # Local scan history state & pre-configured demo test samples
│
├── domain/                              # Business Logic & On-Device Engine
│   ├── engine/
│   │   └── LocalDetectionEngine.kt      # Multi-language (Gujarati, Hindi, Hinglish, English) pattern rules & URL engine
│   └── ocr/
│       └── OcrHelper.kt                 # 100% On-device ML Kit Text Recognition helper
│
└── ui/                                  # UI Layer (Jetpack Compose + Material 3)
    ├── theme/
    │   ├── Color.kt                     # Warm neutral light theme palette
    │   ├── Theme.kt                     # Material 3 light/dark color scheme
    │   └── Type.kt                      # Typography definitions
    │
    ├── components/
    │   └── CommonComponents.kt          # OfflineStatusBadge, RiskScoreBadge, ScanOptionItem, CategoryChip, BottomNavBar
    │
    ├── dashboard/
    │   └── DashboardScreen.kt           # Home screen with 4 Primary Action Cards & Offline Status Component
    │
    ├── scan/
    │   └── ScanScreen.kt                # Scanner supporting Message, URL, Screenshot OCR, and Photo OCR + Camera
    │
    ├── analysis/
    │   └── AnalysisScreen.kt            # Animated step-by-step radar visualizer
    │
    ├── result/
    │   └── ResultScreen.kt              # Common reusable Risk Verdict UI (Score %, Why Flagged, Recommended Action)
    │
    ├── history/
    │   └── HistoryScreen.kt             # Searchable/filterable scan activity log with Clear History confirmation
    │
    └── privacy/
        └── PrivacySettingsScreen.kt     # Privacy checklist & Phase 1 Offline vs Phase 2 Online roadmap
```

---

## 📄 File Roles & Direct Links

| File | Path / Link | Role |
| :--- | :--- | :--- |
| **MainActivity** | [`MainActivity.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/MainActivity.kt) | Manages bottom navigation tabs (**Home**, **Scan**, **History**, **Privacy**) and active screen state. |
| **ScanModels** | [`ScanModels.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/data/model/ScanModels.kt) | Models for `ContentType` (`MESSAGE`, `URL`, `SCREENSHOT`, `PHOTO`), `RiskLevel`, `RiskCategory`, and `ScanResult`. |
| **LocalDetectionEngine** | [`LocalDetectionEngine.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/domain/engine/LocalDetectionEngine.kt) | Analyzes urgency, banking impersonation, payment requests, and suspicious links across **Gujarati**, **Hindi**, **Hinglish**, and **English**. |
| **OcrHelper** | [`OcrHelper.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/domain/ocr/OcrHelper.kt) | Bundled ML Kit Text Recognition helper extracting text from Uri 100% on-device. |
| **ScanRepository** | [`ScanRepository.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/data/repository/ScanRepository.kt) | Stores local scan activity flow and pre-loaded test samples ("Gujarati KYC", "Hindi UPI", "Electricity", "Prize Scam"). |
| **DashboardScreen** | [`DashboardScreen.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/ui/dashboard/DashboardScreen.kt) | Home screen displaying **4 Primary Action Items** (Message, URL, Screenshot, Photo) and Offline Mode checklist. |
| **ScanScreen** | [`ScanScreen.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/ui/scan/ScanScreen.kt) | Input screen supporting text paste, sample loaders, gallery picker, camera photo capture, and OCR extraction. |
| **AnalysisScreen** | [`AnalysisScreen.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/ui/analysis/AnalysisScreen.kt) | Animated step-by-step scanner visualizer. |
| **ResultScreen** | [`ResultScreen.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/ui/result/ResultScreen.kt) | Single reusable Risk Verdict screen displaying score %, "Why Flagged", "What should you do?", and expandable technical details. |
| **HistoryScreen** | [`HistoryScreen.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/ui/history/HistoryScreen.kt) | Activity log with filters (`[ All ]`, `[ Safe ]`, `[ Suspicious ]`, `[ High Risk ]`) and Clear History dialog. |
| **PrivacySettingsScreen** | [`PrivacySettingsScreen.kt`](file:///E:/AndroidProject/RakshaAI/app/src/main/java/com/rakshaai/security/ui/privacy/PrivacySettingsScreen.kt) | Displays **LOCAL ANALYSIS** checklist and Phase 1 vs Phase 2 feature roadmap. |

---

## 🚀 How to Run & Test (100% Offline Verification)

1. Open project in **Android Studio**.
2. Run **Gradle Sync** (`File -> Sync Project with Gradle Files`).
3. Turn **Airplane Mode ON** on your Android device or emulator.
4. Run the app:
   - **Message Scan**: Test "Gujarati KYC" or "Hindi UPI" samples.
   - **URL Scan**: Test a link like `http://paytm-claim.xyz`.
   - **Screenshot Scan**: Pick a screenshot from gallery to see on-device OCR in action.
   - **Photo Scan**: Select or capture a photo with camera for local analysis.
5. All scans execute instantly with zero network access!
