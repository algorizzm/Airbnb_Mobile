# 🔥 FIREBASE TEST DATA

## Quick Setup Guide

### Step 1: Open Firebase Console
1. Go to https://console.firebase.google.com
2. Select your project
3. Navigate to Firestore Database
4. Click on "Start collection" or select existing `listings` collection

### Step 2: Add Test Listings

Click "Add document" for each listing below.

---

## 📦 TEST LISTING 1: Beach House

**Document ID:** Auto-generate or use: `test_listing_beach_001`

### Fields to Add:

| Field | Type | Value |
|-------|------|-------|
| title | string | Cozy Beach House |
| description | string | Beautiful beachfront property with stunning ocean views. Perfect for families and couples looking for a relaxing getaway. |
| location | string | Boracay, Philippines |
| pricePerNight | number | 3500 |
| hostId | string | test_host_1 |
| hostName | string | Maria Santos |
| imageUrl | string | (leave empty) |
| galleryImageUrls | array | (empty array) |
| amenities | array | WiFi, Air Conditioning, Kitchen, Beach Access, Parking |
| maxGuests | number | 4 |
| bedrooms | number | 2 |
| bathrooms | number | 1 |
| propertyType | string | House |
| createdAt | timestamp | (click "Set to current time") |
| updatedAt | timestamp | (click "Set to current time") |

### Copy-Paste JSON (Alternative Method):
```json
{
  "title": "Cozy Beach House",
  "description": "Beautiful beachfront property with stunning ocean views. Perfect for families and couples looking for a relaxing getaway.",
  "location": "Boracay, Philippines",
  "pricePerNight": 3500,
  "hostId": "test_host_1",
  "hostName": "Maria Santos",
  "imageUrl": "",
  "galleryImageUrls": [],
  "amenities": ["WiFi", "Air Conditioning", "Kitchen", "Beach Access", "Parking"],
  "maxGuests": 4,
  "bedrooms": 2,
  "bathrooms": 1,
  "propertyType": "House"
}
```
*Note: Add createdAt and updatedAt timestamps manually in Firebase Console*

---

## 📦 TEST LISTING 2: City Apartment

**Document ID:** Auto-generate or use: `test_listing_city_002`

### Fields to Add:

| Field | Type | Value |
|-------|------|-------|
| title | string | Modern City Apartment |
| description | string | Stylish apartment in the heart of Manila. Close to shopping malls, restaurants, and public transportation. |
| location | string | Makati, Manila |
| pricePerNight | number | 2500 |
| hostId | string | test_host_2 |
| hostName | string | Juan Dela Cruz |
| imageUrl | string | (leave empty) |
| galleryImageUrls | array | (empty array) |
| amenities | array | WiFi, Air Conditioning, Gym Access, 24/7 Security |
| maxGuests | number | 2 |
| bedrooms | number | 1 |
| bathrooms | number | 1 |
| propertyType | string | Apartment |
| createdAt | timestamp | (click "Set to current time") |
| updatedAt | timestamp | (click "Set to current time") |

### Copy-Paste JSON:
```json
{
  "title": "Modern City Apartment",
  "description": "Stylish apartment in the heart of Manila. Close to shopping malls, restaurants, and public transportation.",
  "location": "Makati, Manila",
  "pricePerNight": 2500,
  "hostId": "test_host_2",
  "hostName": "Juan Dela Cruz",
  "imageUrl": "",
  "galleryImageUrls": [],
  "amenities": ["WiFi", "Air Conditioning", "Gym Access", "24/7 Security"],
  "maxGuests": 2,
  "bedrooms": 1,
  "bathrooms": 1,
  "propertyType": "Apartment"
}
```

---

## 📦 TEST LISTING 3: Mountain Villa

**Document ID:** Auto-generate or use: `test_listing_villa_003`

### Fields to Add:

| Field | Type | Value |
|-------|------|-------|
| title | string | Mountain View Villa |
| description | string | Spacious villa with breathtaking mountain views. Ideal for large groups and family reunions. |
| location | string | Tagaytay, Cavite |
| pricePerNight | number | 5000 |
| hostId | string | test_host_1 |
| hostName | string | Maria Santos |
| imageUrl | string | (leave empty) |
| galleryImageUrls | array | (empty array) |
| amenities | array | WiFi, Pool, Garden, BBQ Area, Parking |
| maxGuests | number | 8 |
| bedrooms | number | 4 |
| bathrooms | number | 3 |
| propertyType | string | Villa |
| createdAt | timestamp | (click "Set to current time") |
| updatedAt | timestamp | (click "Set to current time") |

### Copy-Paste JSON:
```json
{
  "title": "Mountain View Villa",
  "description": "Spacious villa with breathtaking mountain views. Ideal for large groups and family reunions.",
  "location": "Tagaytay, Cavite",
  "pricePerNight": 5000,
  "hostId": "test_host_1",
  "hostName": "Maria Santos",
  "imageUrl": "",
  "galleryImageUrls": [],
  "amenities": ["WiFi", "Pool", "Garden", "BBQ Area", "Parking"],
  "maxGuests": 8,
  "bedrooms": 4,
  "bathrooms": 3,
  "propertyType": "Villa"
}
```

---

## 🎯 EXPECTED TEST RESULTS

### Search Tests
- Search "Beach" → Shows Listing 1 only
- Search "Manila" → Shows Listing 2 only
- Search "Tagaytay" → Shows Listing 3 only
- Search "Villa" → Shows Listing 3 only

### Filter Tests
- Max price ₱3000 → Shows Listing 2 only
- Max price ₱4000 → Shows Listings 1 & 2
- Max price ₱6000 → Shows all 3 listings
- Min guests 5 → Shows Listing 3 only
- Min guests 3 → Shows Listings 1 & 3

### Price Calculations
**Listing 1 (Beach House - ₱3500/night):**
- 1 night = ₱3,500
- 2 nights = ₱7,000
- 3 nights = ₱10,500

**Listing 2 (City Apartment - ₱2500/night):**
- 1 night = ₱2,500
- 2 nights = ₱5,000
- 3 nights = ₱7,500

**Listing 3 (Villa - ₱5000/night):**
- 1 night = ₱5,000
- 2 nights = ₱10,000
- 3 nights = ₱15,000

---

## 🔍 VERIFICATION CHECKLIST

After adding test data:

### In Firebase Console
- [ ] Navigate to Firestore Database
- [ ] Open `listings` collection
- [ ] Verify 3 documents exist
- [ ] Each document has all required fields
- [ ] Timestamps are set correctly

### In App
- [ ] Open Explore screen
- [ ] All 3 listings appear
- [ ] Titles match Firebase data
- [ ] Prices match Firebase data
- [ ] Locations match Firebase data

---

## 🧹 CLEANUP (After Testing)

### Option 1: Keep Test Data
Leave the test listings for future testing

### Option 2: Delete Test Data
1. Go to Firebase Console → Firestore
2. Select `listings` collection
3. Delete documents:
   - test_listing_beach_001
   - test_listing_city_002
   - test_listing_villa_003

### Option 3: Delete Test Reservations Only
1. Go to Firebase Console → Firestore
2. Select `reservations` collection
3. Delete any test reservations created during testing

---

## 📝 ADDITIONAL TEST DATA (Optional)

### More Listings for Extended Testing

**Listing 4: Budget Hostel**
```json
{
  "title": "Budget Backpacker Hostel",
  "description": "Affordable accommodation for solo travelers and backpackers.",
  "location": "Cebu City, Cebu",
  "pricePerNight": 800,
  "hostId": "test_host_3",
  "hostName": "Pedro Reyes",
  "imageUrl": "",
  "galleryImageUrls": [],
  "amenities": ["WiFi", "Shared Kitchen", "Lockers"],
  "maxGuests": 1,
  "bedrooms": 1,
  "bathrooms": 1,
  "propertyType": "Hostel"
}
```

**Listing 5: Luxury Condo**
```json
{
  "title": "Luxury Penthouse Condo",
  "description": "High-end penthouse with panoramic city views and premium amenities.",
  "location": "BGC, Taguig",
  "pricePerNight": 8000,
  "hostId": "test_host_2",
  "hostName": "Juan Dela Cruz",
  "imageUrl": "",
  "galleryImageUrls": [],
  "amenities": ["WiFi", "Air Conditioning", "Pool", "Gym", "Concierge", "Parking"],
  "maxGuests": 6,
  "bedrooms": 3,
  "bathrooms": 2,
  "propertyType": "Condominium"
}
```

---

## 🎓 TIPS

### Adding Data Quickly
1. Use Firebase Console's "Add document" feature
2. Copy-paste JSON into a text editor
3. Add fields one by one (faster than typing)
4. Use "Set to current time" for timestamps

### Field Types Reference
- **string:** Text data
- **number:** Numeric data (integers or decimals)
- **array:** List of items (click "+" to add items)
- **timestamp:** Date/time (use "Set to current time" button)

### Common Mistakes to Avoid
❌ Forgetting to add timestamps  
❌ Using wrong field types (e.g., string instead of number for price)  
❌ Misspelling field names  
❌ Leaving required fields empty  

---

**Test Data Setup Complete!** ✅

Now you can proceed with the testing guide.
