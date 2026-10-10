# RakshaAI — Phase 1 Dataset Preparation Report

## 1. Phase 1 Goal

Phase 1 prepared reliable datasets for RakshaAI's scam and phishing detection system.
The datasets were cleaned, checked for duplicates and leakage, and organized for
future model development.

## 2. Datasets Collected

| Dataset | Purpose |
|---|---|
| ScamShield Train | Main dataset for model learning |
| ScamShield Validation | Used to validate model development |
| ScamShield Test | Used for final evaluation |
| India supplementary dataset | Adds India-focused scam message examples |
| UCI SMS Spam dataset | Adds general spam and legitimate SMS examples |
| PhishTank URL dataset | Provides phishing URL examples |

## 3. Dataset Cleaning

The cleaning process:

- Removed missing or empty message text.
- Removed duplicate records where applicable.
- Removed completely empty feature columns.
- Removed duplicate URLs from the PhishTank data.
- Preserved the original raw datasets.

| Dataset | Before | After |
|---|---:|---:|
| ScamShield Train | 69,286 | 69,285 |
| ScamShield Validation | 8,192 | 8,192 |
| ScamShield Test | 8,202 | 8,202 |
| India supplementary | 2,272 | 2,271 |
| UCI SMS Spam | 5,574 | 5,160 |
| PhishTank URLs | 72,159 | 72,156 |

## 4. Leakage Removal

Dataset leakage occurs when the same or highly similar records appear in
datasets that should be kept separate. We checked for leakage to prevent
overlapping supplementary examples from affecting later model evaluation.

- India messages overlapping with ScamShield removed: **2,042**
- Final India supplementary messages: **229**
- UCI messages overlapping with ScamShield removed: **5,155**
- UCI messages overlapping with cleaned India data removed: **1**
- Final UCI supplementary messages: **4**
- Official ScamShield train, validation, and test datasets were **not modified**.

## 5. Train/Validation/Test Inspection

The official ScamShield splits were inspected without deleting or changing
records:

| Overlap | Count |
|---|---:|
| Train ↔ Validation | 12 |
| Train ↔ Test | 13 |
| Validation ↔ Test | 2 |
| **Total** | **27** |

- Same-label overlaps: **27**
- Conflicting-label overlaps: **0**

All 27 overlapping messages had the same labels in the splits where they
appeared. No conflicting-label problem was found.

## 6. Phase 1 Final Result

- Required scam, spam, and phishing datasets were collected and organized.
- Message and URL datasets were cleaned.
- Supplementary dataset leakage was removed.
- Official ScamShield splits were preserved.
- Official train/validation/test overlaps were inspected.
- Phase 1 dataset preparation was completed without modifying source data.

## 7. Next Phase

Phase 1 is complete. The project can move toward **Phase 2: feature preparation
and baseline model development**.
