# FitApp

An Android application that integrates with Google Fit to display daily step count data.

## Features

- 🔐 **Google Fit Integration** - Secure authentication with Google Fit API
- 📊 **Step Count Display** - Real-time daily step count
- 🔄 **Manual Refresh** - Update step data on demand
- 📱 **Modern UI** - Clean, Material Design interface
- ⚡ **Real-time Updates** - Shows last updated timestamp

## Setup Instructions

### Prerequisites

1. **Android Studio Narwhal 4** or later
2. **Google Cloud Console** account
3. **Android device** or emulator with Google Fit installed

### Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable **Google Fit API**
4. Create **OAuth 2.0 Client ID** for Android:
   - **Package name**: `com.fitapp.stepcounter`
   - **SHA-1 fingerprint**: `D7:3E:2C:17:AF:6C:F6:9F:D8:A1:2B:50:E4:6B:F9:3F:F8:57:AC:E5`
5. Download `google-services.json` and place it in `app/` directory

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/jamesshastry/FitApp.git
   cd FitApp
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Open the FitApp project
   - Let Android Studio sync dependencies

3. **Build and Run**:
   - Connect Android device or start emulator
   - Click Run button or use `./gradlew installDebug`

### Permissions Required

- `ACTIVITY_RECOGNITION` - To access step count data
- `ACCESS_FINE_LOCATION` - For Google Fit location services
- `ACCESS_COARSE_LOCATION` - For Google Fit location services
- `BODY_SENSORS` - To access fitness sensor data

### Usage

1. **Launch the app**
2. **Grant permissions** when prompted
3. **Sign in with Google** account
4. **View your daily step count**
5. **Tap refresh** to update step data

## Technical Details

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with LiveData
- **Dependencies**: Google Fit API, Google Play Services

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License.
