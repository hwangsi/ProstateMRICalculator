# ProstateMRICalculator

Android app that calculates prostate volume from MRI images using on-device AI OCR. Designed to assist clinicians in assessing benign prostatic hyperplasia (BPH) severity.

---

## Features

- **AI-Powered OCR**: Automatically extracts width, height, and depth measurements (mm) from MRI images using Google ML Kit — runs entirely on-device, no internet required
- **Image Preprocessing**: Yellow pixel masking to isolate MRI measurement overlays before OCR analysis. The measurement should be in Yellow color!
- **Volume Calculation**: Computes prostate volume using the ellipsoid formula
- **BPH Classification**: Color-coded severity classification across 5 clinical categories
- **Manual Override**: Edit extracted measurements manually if OCR result is incomplete
- **Camera & Gallery**: Load MRI images from gallery or capture directly with camera

---

## Volume Formula

```
V = W × H × D × π / 6 / 1000
```

| Variable | Description |
|----------|-------------|
| W | Width (mm) |
| H | Height (mm) |
| D | Depth (mm) |
| V | Volume (mL / cc) |

---

## BPH Classification

| Volume | Category | Indicator |
|--------|----------|-----------|
| < 20 mL | Below Normal | Blue |
| 20 – 30 mL | Normal | Green |
| 30 – 50 mL | Mild BPH | Amber |
| 50 – 80 mL | Moderate BPH | Orange |
| > 80 mL | Severe BPH | Red |

---

## Tech Stack

| Category | Library / Tool |
|----------|----------------|
| Language | Kotlin 1.9.20 |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM + StateFlow |
| OCR | ML Kit Text Recognition 16.0.0 (on-device) |
| Image Loading | Coil 2.5.0 |
| Navigation | Navigation Compose 2.7.5 |
| Async | Kotlin Coroutines 1.7.3 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Architecture

```
com.prostatemri.calculator
├── MainActivity.kt              # NavHost entry point
├── data/
│   ├── ApiService.kt            # OCR image processing & ML Kit integration
│   ├── SecureStorage.kt
│   └── models/
│       └── ApiModels.kt         # Data classes, BPH category definitions
├── ui/
│   ├── screens/
│   │   ├── MainScreen.kt        # Main UI: image input, OCR, calculation, result
│   │   └── SettingsScreen.kt    # App info & formula reference
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── viewmodel/
    ├── MainViewModel.kt         # UI state, OCR trigger, volume calculation
    └── SettingsViewModel.kt
```

---

## How It Works

1. **Load Image** — Select from gallery or take a photo (max 7 MB)
2. **AI Analysis** — Image is preprocessed (yellow mask → binary) and passed to ML Kit OCR
3. **Value Extraction** — Regex parses `mm`-unit values; top 3 assigned to W, H, D in descending order
4. **Calculate** — Volume computed and displayed with BPH severity category
5. **Manual Edit** — Any measurement field can be edited before or after analysis

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `CAMERA` | Capture MRI image directly |
| `READ_MEDIA_IMAGES` (API 33+) | Access gallery images |
| `READ_EXTERNAL_STORAGE` (API < 33) | Access gallery images |

---

## Getting Started

1. Clone the repository
   ```bash
   git clone https://github.com/hwangsi/ProstateMRICalculator.git
   ```
2. Open in Android Studio (Hedgehog or later)
3. Build & run on a device or emulator (API 26+)

> No API keys or external services required. All processing is on-device.

---

## Disclaimer

This app is intended as a **clinical reference tool** to assist medical professionals. It is not a substitute for professional medical judgment or diagnosis.
