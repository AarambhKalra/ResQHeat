# ResQHeat

**ResQHeat** is an Android emergency response and disaster relief application that connects victims in need with NGOs (Non-Governmental Organizations) through a real-time, map-based platform. The app facilitates efficient coordination during emergencies by enabling victims to request help and NGOs to respond to those requests.

## 🎯 Features

### For Victims
- **Create Requests**: Submit rescue or resource requests with location, priority, and details
- **Real-time Updates**: Receive notifications when requests are accepted or fulfilled
- **Track Requests**: View status of all submitted requests (Pending, In Progress, Completed)
- **Safe Shelters**: Browse and navigate to nearby safe shelters with capacity information
- **Location-based**: Automatic location detection or manual map selection
- **Profile Management**: Store and update personal information

### For NGOs
- **Browse Requests**: View all rescue and resource requests on an interactive map
- **Filter Requests**: Filter by request type (Rescue/Resource) and priority
- **Accept Requests**: Claim and manage requests with status updates
- **Notifications**: Get notified about new requests and high-priority requests within 50km radius
- **Complete Requests**: Mark requests as completed with estimated days covered
- **Victim Contact**: Access victim information for accepted requests

### Map Features
- **Interactive Google Maps**: Full map integration with clustering for better performance
- **Heatmap Visualization**: Visual representation of request density weighted by priority
- **Marker Clustering**: Efficient display of multiple requests
- **Safe Shelter Markers**: Blue markers for active safe shelters with detailed information
- **Location Services**: Real-time location tracking and navigation

### Notifications
- Request accepted notifications (for victims)
- Request fulfilled notifications (for victims)
- New request notifications (for NGOs within 50km)
- High-priority request alerts (for NGOs)

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Backend**: Firebase
  - Authentication (Anonymous)
  - Cloud Firestore (Real-time database)
- **Maps**: Google Maps SDK for Android
  - Maps Compose
  - Maps Utils (Clustering, Heatmaps)
  - Location Services
- **Architecture**: MVVM-inspired with Repository pattern
- **Coroutines**: Kotlin Coroutines for asynchronous operations
- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 36 (Android 14+)

## 📋 Prerequisites

Before running the app, ensure you have:

1. **Android Studio** (latest version recommended)
2. **JDK 11** or higher
3. **Android SDK** (API 28+)
4. **Firebase Project** with:
   - Firestore Database enabled
   - Anonymous Authentication enabled
   - Google Maps API key configured

## 🚀 Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd ResQHeat
```

### 2. Firebase Configuration

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Enable **Firestore Database** (start in test mode or configure security rules)
3. Enable **Anonymous Authentication**:
   - Go to Authentication → Sign-in method
   - Enable "Anonymous" provider
4. Get your **Google Maps API Key**:
   - Go to [Google Cloud Console](https://console.cloud.google.com)
   - Create a project or select existing
   - Enable "Maps SDK for Android"
   - Create an API key
5. Download `google-services.json` and place it in `app/` directory
6. Add your Google Maps API key to `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="google_maps_key">YOUR_API_KEY_HERE</string>
   ```

### 3. Firestore Security Rules

Configure Firestore security rules in Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Safe shelters - public read, authenticated write
    match /safeShelters/{shelterId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Requests - authenticated users only
    match /requests/{requestId} {
      allow read, write: if request.auth != null;
    }
    
    // User profiles - users can read/write their own
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

> **Note**: For development/testing, you can temporarily use `allow read, write: if true;` but **never use this in production**.

### 4. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files
3. Connect an Android device or start an emulator (with Google Play Services)
4. Run the app

## 📁 Project Structure

```
app/src/main/java/aarambh/apps/resqheat/
├── data/
│   ├── FirestoreRepository.kt      # Firestore operations
│   ├── SafeShelterDataUploader.kt   # Helper for uploading shelters
│   └── UserRepository.kt            # User profile operations
├── model/
│   ├── Request.kt                   # Request data model
│   ├── SafeShelter.kt               # Safe shelter data model
│   └── UserProfile.kt               # User profile data model
├── ui/
│   ├── auth/
│   │   └── RolePickerDialog.kt     # Role selection dialog
│   ├── home/
│   │   ├── HomeScreen.kt            # Main map and request list screen
│   │   └── RequestFormDialog.kt    # Request creation form
│   ├── profile/
│   │   └── ProfileScreen.kt        # User profile screen
│   └── theme/
│       ├── Color.kt                 # App color scheme
│       ├── Theme.kt                 # Material 3 theme
│       └── Type.kt                  # Typography
├── utils/
│   └── NotificationHelper.kt        # Notification management
└── MainActivity.kt                  # Main activity and navigation
```

## 🔑 Key Components

### Request Model
- **Types**: RESCUE, RESOURCE
- **Priority**: HIGH, MEDIUM, LOW
- **Status**: NOT_SERVED, BEING_SERVED, SERVED
- **Location**: Latitude/Longitude coordinates
- **Metadata**: Created by, claimed by, ETA, timestamps

### User Roles
- **VICTIM**: Can create requests, view own requests, find safe shelters
- **NGO_ORG**: Can browse all requests, accept/complete requests, filter by type

### Safe Shelters
- Displayed as blue markers on the map
- Show capacity, availability, contact information
- Direct navigation via Google Maps
- Facilities list (Food, Medical, Water, etc.)

## 📱 Usage

### First Launch
1. App automatically signs in anonymously
2. User selects role (Victim or NGO)
3. User provides name and phone number
4. Profile is saved and user enters the app

### Creating a Request (Victims)
1. Tap the floating action button (+)
2. Fill in request details:
   - Type (Rescue/Resource)
   - Resource type (if applicable)
   - Title
   - Priority (High/Medium/Low)
   - Notes
   - Location (auto-detected or long-press on map)
3. Submit request

### Accepting a Request (NGOs)
1. Browse requests on map or list
2. Tap on a request marker or list item
3. View request details and victim information
4. Tap "Accept Request"
5. Status updates to "In Progress" and victim is notified

### Completing a Request (NGOs)
1. Open an accepted request
2. Optionally enter "Estimated Days Covered"
3. Tap "Mark Completed"
4. Status updates to "Completed" and victim is notified

## 🔔 Notifications

The app uses Android notifications for real-time updates:

- **Request Accepted**: Victims receive notification when their request is accepted
- **Request Fulfilled**: Victims receive notification when request is completed
- **New Requests**: NGOs receive notification for new requests within 50km
- **High Priority Alerts**: NGOs receive special notifications for urgent requests

> **Note**: Notification permission is required for Android 13+ (API 33+)

## 🗺️ Safe Shelters Setup

See [SAFE_SHELTERS_SETUP.md](SAFE_SHELTERS_SETUP.md) for detailed instructions on adding safe shelters to the database.

## 🐛 Troubleshooting

### Common Issues

**Firestore Permission Denied**
- Check security rules in Firebase Console
- Ensure anonymous authentication is enabled
- See [FIRESTORE_RULES_FIX.md](FIRESTORE_RULES_FIX.md)

**Shelters Not Appearing**
- Verify shelters are uploaded to Firestore
- Check `isActive` field is `true`
- Ensure coordinates are valid (not 0.0, 0.0)
- See [TROUBLESHOOTING_UPLOAD.md](TROUBLESHOOTING_UPLOAD.md)

**Maps Not Loading**
- Verify Google Maps API key is configured correctly
- Check API key has "Maps SDK for Android" enabled
- Ensure billing is enabled in Google Cloud Console

**Location Not Working**
- Grant location permissions when prompted
- Enable location services on device
- Check app has location permissions in device settings

## 🔒 Permissions

The app requires the following permissions:
- `INTERNET`: For Firebase and Maps connectivity
- `ACCESS_FINE_LOCATION`: For precise location tracking
- `ACCESS_COARSE_LOCATION`: For approximate location
- `POST_NOTIFICATIONS`: For notifications (Android 13+)

## 📝 License

[Add your license information here]

## 🤝 Contributing

[Add contribution guidelines here]

## 📧 Contact

[Add contact information here]

## 🙏 Acknowledgments

- Firebase for backend services
- Google Maps for mapping functionality
- Material 3 for UI components
- Jetpack Compose team for the modern UI framework

---

**Built with ❤️ for emergency response coordination**

