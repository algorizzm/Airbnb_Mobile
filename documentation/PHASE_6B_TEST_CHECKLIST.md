# 🧪 PHASE 6B — HOSTING TEST CHECKLIST

## 📋 QUICK TEST GUIDE

Use this checklist to verify Phase 6B hosting functionality.

---

## ✅ TEST 1: PROFILE → HOST LISTINGS NAVIGATION

### Steps
1. Open the app
2. Navigate to **Profile** tab (bottom navigation)
3. Scroll down to **Hosting** card
4. Tap **"Manage Listings"** button

### Expected Results
- ✅ Navigates to Host Listings screen
- ✅ Toolbar shows "My Listings"
- ✅ Back button visible in toolbar
- ✅ FAB (+ button) visible at bottom right

### If No Listings Exist
- ✅ Empty state displays
- ✅ Shows house icon
- ✅ Shows "No listings yet" message
- ✅ Shows "Create your first listing to start hosting" text

---

## ✅ TEST 2: CREATE NEW LISTING

### Steps
1. From Host Listings screen
2. Tap **FAB (+ button)**
3. Fill in the form:
   - **Title:** "Cozy Beach House"
   - **Description:** "Beautiful beachfront property with stunning views"
   - **Location:** "Malibu, California"
   - **Property Type:** Select "Entire home"
   - **Price:** "150"
   - **Max Guests:** Tap + to set to 4
   - **Bedrooms:** Tap + to set to 2
   - **Bathrooms:** Tap + to set to 2
   - **Amenities:** "WiFi, Kitchen, Pool, Beach access"
   - **Image URL:** (leave blank or paste any image URL)
4. Tap **"Save Listing"**

### Expected Results
- ✅ Button shows "Saving..." during save
- ✅ Toast shows "Listing created!"
- ✅ Navigates back to Host Listings screen
- ✅ New listing appears in RecyclerView
- ✅ Listing shows correct title, location, price
- ✅ Empty state is hidden

---

## ✅ TEST 3: VIEW HOST LISTINGS

### Steps
1. From Host Listings screen
2. Observe the listing card

### Expected Results
- ✅ Listing image displays (or placeholder)
- ✅ Title displays correctly
- ✅ Location displays correctly
- ✅ Property type displays ("Entire home")
- ✅ Price displays as "$150 / night"
- ✅ Three buttons visible: Edit, Delete, Bookings

---

## ✅ TEST 4: EDIT EXISTING LISTING

### Steps
1. From Host Listings screen
2. Tap **"Edit"** button on a listing
3. Verify form is pre-filled with existing data
4. Change **Title** to "Updated Beach House"
5. Change **Price** to "175"
6. Tap **"Save Listing"**

### Expected Results
- ✅ Form pre-fills with existing listing data
- ✅ Toolbar shows "Edit Listing"
- ✅ All fields editable
- ✅ Property type spinner shows correct selection
- ✅ Counters show correct values
- ✅ Toast shows "Listing updated!"
- ✅ Navigates back to Host Listings
- ✅ Listing shows updated title and price

---

## ✅ TEST 5: DELETE LISTING

### Steps
1. From Host Listings screen
2. Tap **"Delete"** button on a listing
3. Observe confirmation dialog
4. Tap **"Delete"** in dialog

### Expected Results
- ✅ Confirmation dialog appears
- ✅ Dialog shows listing title
- ✅ Dialog warns about cancelling reservations
- ✅ Dialog has "Delete" and "Cancel" buttons
- ✅ After confirming, toast shows "Listing deleted successfully"
- ✅ Listing disappears from RecyclerView
- ✅ If last listing, empty state appears

---

## ✅ TEST 6: VIEW RESERVATIONS (NO BOOKINGS)

### Steps
1. From Host Listings screen
2. Tap **"Bookings"** button on a listing

### Expected Results
- ✅ Navigates to Host Reservations screen
- ✅ Toolbar shows "Reservations"
- ✅ Back button visible
- ✅ Empty state displays
- ✅ Shows calendar icon
- ✅ Shows "No reservations yet" message
- ✅ Shows "Reservations for this listing will appear here" text

---

## ✅ TEST 7: VIEW RESERVATIONS (WITH BOOKINGS)

### Prerequisites
- Create a reservation for the listing as a guest first
- (Switch to guest view, find listing, make reservation)

### Steps
1. From Host Listings screen
2. Tap **"Bookings"** button on a listing with reservations

### Expected Results
- ✅ Navigates to Host Reservations screen
- ✅ Reservation cards display
- ✅ Guest name shows correctly
- ✅ Check-in/check-out dates display
- ✅ Number of guests displays
- ✅ Total price displays
- ✅ Status badge shows (Pending/Confirmed)
- ✅ Status color is green for active reservations
- ✅ Cancel button visible for pending/confirmed

---

## ✅ TEST 8: CANCEL RESERVATION AS HOST

### Prerequisites
- Listing has at least one pending/confirmed reservation

### Steps
1. From Host Reservations screen
2. Tap **"Cancel"** button on a reservation
3. Observe confirmation dialog
4. Tap **"Cancel Reservation"** in dialog

### Expected Results
- ✅ Confirmation dialog appears
- ✅ Dialog shows guest name
- ✅ Dialog has "Cancel Reservation" and "Keep" buttons
- ✅ After confirming, toast shows "Reservation cancelled"
- ✅ Status badge updates to "Cancelled"
- ✅ Status color changes to red
- ✅ Cancel button disappears

---

## ✅ TEST 9: FORM VALIDATION

### Steps
1. From Host Listings screen
2. Tap FAB to create new listing
3. Leave **Title** blank
4. Tap **"Save Listing"**
5. Fill **Title**, leave **Description** blank
6. Tap **"Save Listing"**
7. Fill **Description**, leave **Location** blank
8. Tap **"Save Listing"**
9. Fill **Location**, set **Price** to "0"
10. Tap **"Save Listing"**

### Expected Results
- ✅ Toast shows "Title is required" (step 4)
- ✅ Toast shows "Description is required" (step 6)
- ✅ Toast shows "Location is required" (step 8)
- ✅ Toast shows "Price must be greater than 0" (step 10)
- ✅ Form does not submit until all fields valid

---

## ✅ TEST 10: REAL-TIME UPDATES

### Steps
1. Open app on Device A (or emulator)
2. Navigate to Host Listings
3. Open app on Device B (or another emulator) with same account
4. Navigate to Host Listings
5. On Device A, create a new listing
6. Observe Device B

### Expected Results
- ✅ New listing appears on Device B automatically
- ✅ No manual refresh needed
- ✅ Real-time Firestore listener working

---

## ✅ TEST 11: NAVIGATION FLOWS

### Steps
1. Profile → Host Listings → Create Listing → Back → Back → Profile ✅
2. Profile → Host Listings → Edit Listing → Back → Back → Profile ✅
3. Profile → Host Listings → Reservations → Back → Back → Profile ✅
4. Profile → Host Listings → Create → Save → Host Listings ✅

### Expected Results
- ✅ All navigation flows work correctly
- ✅ Back button always returns to previous screen
- ✅ No navigation crashes
- ✅ No orphaned screens

---

## ✅ TEST 12: LOADING STATES

### Steps
1. Navigate to Host Listings
2. Observe loading indicator
3. Create a new listing
4. Observe "Saving..." button state
5. Edit a listing
6. Observe loading while fetching data

### Expected Results
- ✅ Loading spinner shows while fetching listings
- ✅ Button shows "Saving..." during save
- ✅ Button is disabled during save
- ✅ Loading spinner shows while fetching listing for edit
- ✅ Form is hidden during loading

---

## ✅ TEST 13: ERROR HANDLING

### Steps
1. Turn off internet connection
2. Try to create a listing
3. Observe error handling
4. Turn on internet
5. Try again

### Expected Results
- ✅ Toast shows error message
- ✅ App doesn't crash
- ✅ User can retry after fixing connection

---

## ✅ TEST 14: GUEST/BEDROOM/BATHROOM COUNTERS

### Steps
1. Navigate to Create Listing
2. Test Max Guests counter:
   - Tap **-** button (should not go below 1)
   - Tap **+** button multiple times (should increase)
   - Verify count displays correctly
3. Test Bedrooms counter (same as above)
4. Test Bathrooms counter (same as above)

### Expected Results
- ✅ Counters start at 1
- ✅ Cannot decrease below 1
- ✅ Can increase up to 20
- ✅ Count displays correctly
- ✅ +/- buttons work smoothly

---

## ✅ TEST 15: PROPERTY TYPE SPINNER

### Steps
1. Navigate to Create Listing
2. Tap **Property Type** spinner
3. Observe dropdown options
4. Select "Private room"
5. Save listing
6. Edit listing
7. Verify spinner shows "Private room"

### Expected Results
- ✅ Spinner shows 4 options:
  - Entire home
  - Private room
  - Shared room
  - Hotel room
- ✅ Selection saves correctly
- ✅ Edit form shows correct selection

---

## 🎯 PASS CRITERIA

### Minimum Requirements
- [ ] All 15 tests pass
- [ ] No crashes
- [ ] No compilation errors
- [ ] Navigation works correctly
- [ ] Firebase integration works
- [ ] Real-time updates work
- [ ] Form validation works
- [ ] Loading states display
- [ ] Error handling works

### Optional Enhancements (Post-MVP)
- [ ] Image upload from device
- [ ] Multiple image gallery
- [ ] Advanced amenities selection
- [ ] Listing analytics
- [ ] Approve/reject reservations

---

## 📊 TEST RESULTS

### Test Summary
- **Total Tests:** 15
- **Passed:** ___
- **Failed:** ___
- **Skipped:** ___

### Issues Found
1. _____________________
2. _____________________
3. _____________________

### Notes
_____________________
_____________________
_____________________

---

## 🚀 NEXT STEPS AFTER TESTING

### If All Tests Pass
1. ✅ Mark Phase 6B as complete
2. ✅ Update progress documentation
3. ✅ Move to Phase 7 (Profile Updates)

### If Tests Fail
1. 🐛 Document issues
2. 🔧 Fix bugs
3. 🧪 Re-test
4. ✅ Verify fixes

---

**Test Date:** _______________  
**Tester:** _______________  
**Device/Emulator:** _______________  
**Android Version:** _______________  
**Test Status:** ⏳ PENDING / ✅ PASSED / ❌ FAILED

---

## 📝 QUICK FIREBASE VERIFICATION

### Check Firestore Console
1. Open Firebase Console
2. Navigate to Firestore Database
3. Check `listings/` collection
4. Verify new listings appear
5. Check `reservations/` collection
6. Verify status updates when cancelled

### Expected Firebase Structure
```
listings/
  {listingId}/
    - title: "Cozy Beach House"
    - description: "Beautiful beachfront..."
    - location: "Malibu, California"
    - propertyType: "Entire home"
    - pricePerNight: 150
    - maxGuests: 4
    - bedrooms: 2
    - bathrooms: 2
    - amenities: ["WiFi", "Kitchen", "Pool", "Beach access"]
    - hostId: {currentUserId}
    - hostName: {currentUserName}
    - createdAt: {timestamp}
    - updatedAt: {timestamp}
```

---

**Happy Testing! 🎉**
