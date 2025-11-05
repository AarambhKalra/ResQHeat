# Safe Shelters Setup Guide

This guide explains how to add safe shelters to the ResQHeat app database.

## Overview

Safe shelters are displayed on the map with blue icons and can be viewed by both victims and NGOs. When a user clicks on a shelter marker, they can see:
- Shelter name
- Address
- Capacity/Availability
- Contact information
- Facilities available
- Get directions via Google Maps

## Firestore Collection Structure

The shelters are stored in a Firestore collection called `safeShelters` with the following structure:

```json
{
  "id": "auto-generated-document-id",
  "name": "Community Center - Downtown",
  "address": "123 Main Street, Downtown Area",
  "lat": 28.6139,
  "lng": 77.2090,
  "capacity": 200,
  "currentOccupancy": 45,
  "availableSpots": 155,
  "contactPhone": "+91-11-12345678",
  "contactEmail": "downtown-shelter@example.com",
  "facilities": ["Food", "Medical", "Water", "Sanitation", "Beds"],
  "isActive": true,
  "createdAt": 1234567890000,
  "updatedAt": 1234567890000
}
```

## Adding Safe Shelters

### Method 1: Using the Uploader Script (Recommended for initial setup)

1. Open `SafeShelterDataUploader.kt`
2. Modify the `getExampleShelters()` function with your actual shelter data
3. Call the uploader from your app (temporarily) or test:
   ```kotlin
   val uploader = SafeShelterDataUploader()
   lifecycleScope.launch {
       uploader.uploadExampleShelters()
   }
   ```

### Method 2: Using Firebase Console

1. Go to Firebase Console → Firestore Database
2. Create or navigate to the `safeShelters` collection
3. Click "Add document" and fill in the fields:
   - **name** (string): Shelter name
   - **address** (string, optional): Physical address
   - **lat** (number): Latitude coordinate
   - **lng** (number): Longitude coordinate
   - **capacity** (number): Total capacity
   - **currentOccupancy** (number): Current number of people
   - **availableSpots** (number): Available spots (capacity - occupancy)
   - **contactPhone** (string, optional): Contact phone number
   - **contactEmail** (string, optional): Contact email
   - **facilities** (array of strings): Available facilities
   - **isActive** (boolean): Set to `true` to show on map
   - **createdAt** (timestamp): Creation timestamp
   - **updatedAt** (timestamp): Last update timestamp

### Method 3: Using Firebase Admin SDK (For bulk uploads)

If you have many shelters, you can create a script using Firebase Admin SDK:

```javascript
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

const shelters = [
  {
    name: "Community Center - Downtown",
    address: "123 Main Street",
    lat: 28.6139,
    lng: 77.2090,
    capacity: 200,
    currentOccupancy: 45,
    availableSpots: 155,
    contactPhone: "+91-11-12345678",
    facilities: ["Food", "Medical", "Water"],
    isActive: true,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  },
  // Add more shelters...
];

async function uploadShelters() {
  const batch = db.batch();
  
  shelters.forEach(shelter => {
    const docRef = db.collection('safeShelters').doc();
    batch.set(docRef, shelter);
  });
  
  await batch.commit();
  console.log('Shelters uploaded successfully');
}

uploadShelters();
```

## Example Shelter Data

Here are example coordinates for different regions (replace with your actual locations):

### India (Delhi area)
- lat: 28.6139, lng: 77.2090 (Downtown)
- lat: 28.7041, lng: 77.1025 (North)
- lat: 28.5245, lng: 77.1855 (South)

### Finding Coordinates

You can find accurate coordinates using:
1. Google Maps: Right-click on location → Copy coordinates
2. GPS coordinates websites
3. Geocoding APIs

## Updating Shelter Information

To update shelter capacity or availability:

1. Go to Firebase Console → Firestore
2. Open the `safeShelters` collection
3. Find the shelter document
4. Update:
   - `currentOccupancy`: Current number of people
   - `availableSpots`: Recalculate as (capacity - currentOccupancy)
   - `updatedAt`: Update timestamp

## Firestore Security Rules

Make sure your Firestore security rules allow reading shelters (they should be public):

```javascript
match /safeShelters/{shelterId} {
  allow read: if true; // Anyone can read shelters
  allow write: if false; // Only admins can write (configure as needed)
}
```

## Notes

- Only shelters with `isActive: true` are shown on the map
- Shelters with invalid coordinates (lat/lng = 0.0) are filtered out
- The app automatically listens to changes in the Firestore collection
- Blue markers (HUE_AZURE) are used to distinguish shelters from request markers

