# 🔥 FIREBASE INDEXES REQUIRED

## 📋 OVERVIEW

Firebase Firestore requires composite indexes for queries that filter and sort on multiple fields. This document lists all required indexes for the Airbnb MVP app.

---

## ⚠️ IMPORTANT

**When you see this error:**
```
The query requires an index. You can create it here: [URL]
```

**Solution:**
1. Click the URL in the error message
2. Firebase will open with the index pre-configured
3. Click "Create Index"
4. Wait 1-2 minutes for it to build
5. Retry the operation

---

## 📊 REQUIRED INDEXES

### 1. Listings by Host (REQUIRED FOR HOSTING)

**Error Message:**
```
Listen for Query(target=Query(listings where hostId==... order by -createdAt)
```

**Index Configuration:**
- **Collection:** `listings`
- **Fields:**
  - `hostId` → Ascending
  - `createdAt` → Descending
- **Query Scope:** Collection

**Used By:**
- Host Listings screen
- `ListingRepository.observeListingsForHost()`

**Quick Create Link:**
Click the link from your error message, or create manually in Firebase Console.

---

### 2. Reservations by Guest (REQUIRED FOR TRIPS)

**Index Configuration:**
- **Collection:** `reservations`
- **Fields:**
  - `guestId` → Ascending
  - `createdAt` → Descending
- **Query Scope:** Collection

**Used By:**
- Trips screen
- `ReservationRepository.observeReservationsForGuest()`

---

### 3. Reservations by Host (REQUIRED FOR HOST RESERVATIONS)

**Index Configuration:**
- **Collection:** `reservations`
- **Fields:**
  - `hostId` → Ascending
  - `createdAt` → Descending
- **Query Scope:** Collection

**Used By:**
- Host Reservations screen (if viewing all host reservations)
- `ReservationRepository.observeReservationsForHost()`

---

### 4. Reservations by Listing (REQUIRED FOR HOST RESERVATIONS)

**Index Configuration:**
- **Collection:** `reservations`
- **Fields:**
  - `listingId` → Ascending
  - `checkInDate` → Ascending
- **Query Scope:** Collection

**Used By:**
- Host Reservations screen (per listing)
- `ReservationRepository.observeReservationsForListing()`

---

## 🚀 HOW TO CREATE INDEXES

### Method 1: Click Error Link (Recommended)

1. **Run the app** and trigger the feature
2. **Check Logcat** for the error message
3. **Copy the URL** from the error message
4. **Open in browser** (Firebase Console)
5. **Click "Create Index"**
6. **Wait** for index to build (1-2 minutes)

### Method 2: Manual Creation

1. **Open Firebase Console**
   - Go to https://console.firebase.google.com
   - Select your project: `airbnb-mobile-36505`

2. **Navigate to Firestore**
   - Click "Firestore Database" in left menu
   - Click "Indexes" tab

3. **Create Composite Index**
   - Click "Create Index" button
   - Fill in collection and fields
   - Click "Create"

4. **Wait for Build**
   - Status will show "Building..."
   - Wait 1-2 minutes
   - Status will change to "Enabled"

---

## 📝 INDEX CREATION CHECKLIST

Use this checklist to ensure all indexes are created:

- [ ] **Listings by Host**
  - Collection: `listings`
  - Fields: `hostId` (ASC), `createdAt` (DESC)
  
- [ ] **Reservations by Guest**
  - Collection: `reservations`
  - Fields: `guestId` (ASC), `createdAt` (DESC)
  
- [ ] **Reservations by Host**
  - Collection: `reservations`
  - Fields: `hostId` (ASC), `createdAt` (DESC)
  
- [ ] **Reservations by Listing**
  - Collection: `reservations`
  - Fields: `listingId` (ASC), `checkInDate` (ASC)

---

## 🧪 TESTING AFTER INDEX CREATION

### Test 1: Host Listings
1. Navigate to Profile → Manage Listings
2. Should load without errors
3. Create a listing
4. Verify it appears in the list

### Test 2: Trips
1. Navigate to Trips tab
2. Should load without errors
3. Verify trips display correctly

### Test 3: Host Reservations
1. Navigate to Host Listings
2. Tap "Bookings" on a listing
3. Should load without errors
4. Verify reservations display

---

## ⏱️ INDEX BUILD TIME

- **Small datasets** (< 100 documents): ~30 seconds
- **Medium datasets** (100-1000 documents): ~1-2 minutes
- **Large datasets** (> 1000 documents): ~5-10 minutes

**Note:** You can use the app while indexes are building, but queries requiring those indexes will fail until complete.

---

## 🐛 TROUBLESHOOTING

### Issue: Index creation fails

**Solution:**
- Check Firebase billing (indexes require Blaze plan for production)
- Verify you have Owner/Editor permissions
- Try creating manually instead of using the link

### Issue: Index shows "Error" status

**Solution:**
- Delete the index
- Recreate it
- Check field names match exactly (case-sensitive)

### Issue: App still shows error after index created

**Solution:**
- Wait 1-2 more minutes (index might still be building)
- Force close and restart the app
- Check index status in Firebase Console (should be "Enabled")

### Issue: Multiple index errors

**Solution:**
- Create indexes one at a time
- Test each feature after creating its index
- Use the error link for each one (easiest method)

---

## 📊 INDEX MONITORING

### Check Index Status

1. **Firebase Console** → **Firestore Database** → **Indexes**
2. View all indexes and their status:
   - 🟢 **Enabled** - Ready to use
   - 🟡 **Building** - In progress
   - 🔴 **Error** - Failed (needs recreation)

### Index Performance

- Indexes improve query performance
- No negative impact on write operations
- Minimal storage overhead
- Automatically maintained by Firebase

---

## 🎯 QUICK REFERENCE

| Feature | Collection | Fields | Order |
|---------|-----------|--------|-------|
| Host Listings | listings | hostId, createdAt | ASC, DESC |
| Trips (Guest) | reservations | guestId, createdAt | ASC, DESC |
| Host Reservations (All) | reservations | hostId, createdAt | ASC, DESC |
| Host Reservations (Listing) | reservations | listingId, checkInDate | ASC, ASC |

---

## 💡 BEST PRACTICES

### Development
- Create indexes as you encounter errors
- Use the error link (fastest method)
- Test each feature after creating index

### Production
- Create all indexes before launch
- Monitor index usage in Firebase Console
- Delete unused indexes to save resources

### Team Collaboration
- Document all required indexes
- Share this guide with team members
- Keep index list updated as features are added

---

## 🔗 USEFUL LINKS

- **Firebase Console:** https://console.firebase.google.com
- **Firestore Indexes Documentation:** https://firebase.google.com/docs/firestore/query-data/indexing
- **Index Best Practices:** https://firebase.google.com/docs/firestore/query-data/index-overview

---

## ✅ COMPLETION CHECKLIST

After creating all indexes:

- [ ] Host Listings screen loads without errors
- [ ] Trips screen loads without errors
- [ ] Host Reservations screen loads without errors
- [ ] All indexes show "Enabled" status in Firebase Console
- [ ] App tested on multiple devices/emulators
- [ ] No Firestore index errors in Logcat

---

## 🎉 DONE!

Once all indexes are created and enabled, your app will work smoothly without any Firestore index errors.

**Remember:** Always click the error link first—it's the fastest way to create the correct index!

---

**Last Updated:** Phase 6B Implementation  
**Status:** Required for hosting and trips features
