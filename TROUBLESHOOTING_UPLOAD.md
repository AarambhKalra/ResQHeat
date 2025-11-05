# Troubleshooting Safe Shelter Upload

If shelters are not appearing in Firestore, check the following:

## 1. Check Logcat for Errors

Look for these log tags in Android Studio Logcat:
- `MainActivity` - Shows upload start/status
- `SafeShelterDataUploader` - Shows detailed upload progress

**Filter Logcat by:** `SafeShelterDataUploader` or `MainActivity`

## 2. Check Firestore Security Rules

The most common issue is Firestore security rules blocking writes.

Go to Firebase Console → Firestore Database → Rules

**Temporary rule for testing (ALLOW ALL - NOT FOR PRODUCTION):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /safeShelters/{document=**} {
      allow read, write: if true;
    }
    // ... other rules
  }
}
```

**After testing, update to:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /safeShelters/{document=**} {
      allow read: if true;  // Anyone can read
      allow write: if request.auth != null;  // Only authenticated users can write
    }
  }
}
```

## 3. Check Firebase Authentication

Make sure anonymous authentication is enabled:
1. Firebase Console → Authentication → Sign-in method
2. Enable "Anonymous" provider

## 4. Check Internet Connection

The app needs internet to connect to Firestore.

## 5. Verify Collection Name

The collection should be named exactly: `safeShelters` (case-sensitive)

## 6. Check Toast Messages

The app will show:
- ✅ "Safe shelters uploaded successfully!" on success
- ❌ "Failed to upload shelters: [error]" on failure

## 7. Manual Verification

After running the app, check Firebase Console:
1. Go to Firestore Database
2. Look for collection: `safeShelters`
3. Should see 3 documents with shelter names

## Common Error Messages

### "PERMISSION_DENIED"
- **Solution:** Update Firestore security rules (see #2)

### "Network error" or "Failed to connect"
- **Solution:** Check internet connection

### "Collection not found"
- **Solution:** Collection will be created automatically on first write - this is normal

