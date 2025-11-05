# Fix Firestore Permission Denied Error

## Quick Fix Steps:

1. **Go to Firebase Console:**
   - Open https://console.firebase.google.com
   - Select your project
   - Click on **Firestore Database** in the left sidebar
   - Click on the **Rules** tab at the top

2. **Update the Rules:**

   Replace your current rules with this (temporary for testing):

   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       // Allow all operations for safeShelters (for testing)
       match /safeShelters/{document=**} {
         allow read, write: if true;
       }
       
       // Keep your existing rules for other collections
       match /requests/{document=**} {
         allow read, write: if request.auth != null;
       }
       
       match /users/{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

3. **Click "Publish"** to save the rules

4. **Run your app again** - the upload should work now!

## After Testing (Production Rules):

Once you've confirmed the upload works, update to more secure rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Safe shelters - anyone can read, only authenticated users can write
    match /safeShelters/{document=**} {
      allow read: if true;  // Public read
      allow write: if request.auth != null;  // Only authenticated users can write
    }
    
    // Your other collections
    match /requests/{document=**} {
       allow read, write: if request.auth != null;
    }
    
    match /users/{userId} {
       allow read: if request.auth != null;
       allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Alternative: Test Rules (Less Secure - Development Only)

If you want to test quickly without authentication:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

**⚠️ WARNING:** The last rule allows anyone to read/write everything. Only use for development/testing!

