# 🧪 TESTING GUIDE - Phase 2, 3, 4 Verification

## Overview

This guide provides step-by-step manual test cases to verify the functionality implemented in Phases 2, 3, and 4 of the Airbnb MVP refactoring project.

**Test Environment:** Android Emulator or Physical Device  
**Prerequisites:** Firebase project configured, app compiled successfully  
**Estimated Testing Time:** 30-45 minutes  

---

## 🔧 PRE-TEST SETUP

### Step 1: Prepare Firebase Test Data

Before testing, you need sample listings in Firestore. Use Firebase Console to add test data:

#### Create Test Listings

Navigate to Firebase Console → Firestore Database → `listings` collection

**Sample Listing 1:**
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
  "propertyType": "House",
  "createdAt": [Current Timestamp],
  "updatedAt": [Current Timestamp]
}
```

**Sample Listing 2:**
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
  "propertyType": "Apartment",
  "createdAt": [Current Timestamp],
  "updatedAt": [Current Timestamp]
}
```

**Sample Listing 3:**
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
  "propertyType": "Villa",
  "createdAt": [Current Timestamp],
  "updatedAt": [Current Timestamp]
}
```

### Step 2: Build and Install App

```bash
# Clean build
.\gradlew clean

# Build debug APK
.\gradlew assembleDebug

# Install on device/emulator
.\gradlew installDebug
```

### Step 3: Launch App

Open the app on your device/emulator and ensure you're logged in (or create a test account).

---

## 📱 TEST SUITE 1: EXPLORE SCREEN (PHASE 2)

### Test Case 1.1: Display Listings
**Objective:** Verify that property listings are displayed correctly

**Steps:**
1. Launch the app
2. Navigate to the Explore tab (bottom navigation)
3. Wait for listings to load

**Expected Results:**
✅ Loading indicator appears briefly  
✅ All 3 test listings are displayed  
✅ Each card shows:
   - Property title
   - Location
   - Guest capacity (e.g., "4 guests")
   - Price per night (e.g., "₱3500/night")
✅ Cards are in a scrollable list  
✅ No error messages appear  

**Pass/Fail:** ___________

---

### Test Case 1.2: Search Functionality
**Objective:** Verify search by title and location works

**Steps:**
1. On Explore screen, tap the search bar
2. Type "Beach"
3. Observe results

**Expected Results:**
✅ Only "Cozy Beach House" is displayed  
✅ Other listings are filtered out  
✅ Search is case-insensitive  

**Steps (continued):**
4. Clear search
5. Type "Manila"
6. Observe results

**Expected Results:**
✅ Only "Modern City Apartment" is displayed  
✅ Search works for location field  

**Steps (continued):**
7. Clear search
8. Type "xyz123" (non-existent)
9. Observe results

**Expected Results:**
✅ Empty state message appears  
✅ "No listings found" text is visible  

**Pass/Fail:** ___________

---

### Test Case 1.3: Price Filter
**Objective:** Verify max price filter works

**Steps:**
1. On Explore screen, tap "Filters" button
2. Filter panel expands
3. Enter "3000" in "Max Price per Night" field
4. Observe results

**Expected Results:**
✅ Only "Modern City Apartment" (₱2500) is displayed  
✅ Beach House (₱3500) and Villa (₱5000) are hidden  

**Steps (continued):**
5. Change max price to "6000"
6. Observe results

**Expected Results:**
✅ All 3 listings are displayed  

**Pass/Fail:** ___________

---

### Test Case 1.4: Guest Filter
**Objective:** Verify minimum guests filter works

**Steps:**
1. On Explore screen, open Filters
2. Enter "5" in "Minimum Guests" field
3. Observe results

**Expected Results:**
✅ Only "Mountain View Villa" (8 guests) is displayed  
✅ Other listings with fewer guests are hidden  

**Steps (continued):**
4. Change to "2" guests
5. Observe results

**Expected Results:**
✅ All 3 listings are displayed (all have ≥2 guests)  

**Pass/Fail:** ___________

---

### Test Case 1.5: Clear Filters
**Objective:** Verify clear filters button works

**Steps:**
1. On Explore screen, open Filters
2. Set max price to "3000"
3. Set min guests to "5"
4. Tap "Clear filters"
5. Observe results

**Expected Results:**
✅ Both filter fields are cleared  
✅ All listings are displayed again  

**Pass/Fail:** ___________

---

### Test Case 1.6: Navigation to Detail
**Objective:** Verify tapping a listing navigates to detail screen

**Steps:**
1. On Explore screen, tap "Cozy Beach House" card
2. Observe navigation

**Expected Results:**
✅ App navigates to Listing Detail screen  
✅ No crash or error  
✅ Back button is visible  

**Pass/Fail:** ___________

---

## 📱 TEST SUITE 2: LISTING DETAIL SCREEN (PHASE 3)

### Test Case 2.1: Display Listing Information
**Objective:** Verify all listing details are displayed correctly

**Prerequisites:** Navigate to "Cozy Beach House" detail screen

**Expected Results:**
✅ Title: "Cozy Beach House" is displayed  
✅ Location: "Boracay, Philippines" is displayed  
✅ Property Type: "House" is displayed  
✅ Guest capacity: "4 guests" is displayed  
✅ Bedrooms: "2 bedrooms" is displayed  
✅ Bathrooms: "1 bathroom" is displayed  
✅ Host name: "Maria Santos" is displayed  
✅ Description is fully visible  
✅ Amenities list shows all 5 items with bullet points  
✅ Price: "₱3500" is displayed at bottom  
✅ "Reserve" button is visible  

**Pass/Fail:** ___________

---

### Test Case 2.2: Loading State
**Objective:** Verify loading state appears while fetching data

**Steps:**
1. From Explore, tap a listing
2. Observe the detail screen as it loads

**Expected Results:**
✅ Loading spinner appears briefly  
✅ Content appears after loading  
✅ No error state  

**Pass/Fail:** ___________

---

### Test Case 2.3: Back Navigation
**Objective:** Verify back button returns to Explore screen

**Steps:**
1. On Listing Detail screen, tap back button (top left)
2. Observe navigation

**Expected Results:**
✅ App returns to Explore screen  
✅ Listing list is still visible  
✅ No crash  

**Pass/Fail:** ___________

---

### Test Case 2.4: Reserve Button Navigation
**Objective:** Verify reserve button navigates to reservation screen

**Steps:**
1. On Listing Detail screen, scroll to bottom
2. Tap "Reserve" button
3. Observe navigation

**Expected Results:**
✅ App navigates to Create Reservation screen  
✅ Listing summary is visible at top  
✅ No crash or error  

**Pass/Fail:** ___________

---

## 📱 TEST SUITE 3: RESERVATION CREATION (PHASE 4)

### Test Case 3.1: Display Listing Summary
**Objective:** Verify listing information is shown on reservation screen

**Prerequisites:** Navigate to Create Reservation screen from "Cozy Beach House"

**Expected Results:**
✅ Listing image placeholder is visible  
✅ Title: "Cozy Beach House" is displayed  
✅ Location: "Boracay, Philippines" is displayed  
✅ Price: "₱3500/night" is displayed  
✅ All information matches the listing  

**Pass/Fail:** ___________

---

### Test Case 3.2: Check-in Date Picker
**Objective:** Verify check-in date selection works

**Steps:**
1. On Create Reservation screen, tap "Check-in" field
2. Date picker dialog appears
3. Select tomorrow's date
4. Tap OK

**Expected Results:**
✅ Date picker dialog opens  
✅ Today's date is disabled (can't select)  
✅ Tomorrow and future dates are selectable  
✅ Selected date appears in check-in field  
✅ Date format: "MMM dd, yyyy" (e.g., "Jan 15, 2024")  

**Pass/Fail:** ___________

---

### Test Case 3.3: Check-out Date Picker
**Objective:** Verify check-out date selection works

**Steps:**
1. Ensure check-in date is selected
2. Tap "Check-out" field
3. Date picker dialog appears
4. Select a date 3 days after check-in
5. Tap OK

**Expected Results:**
✅ Date picker dialog opens  
✅ Dates before check-in are disabled  
✅ Check-in date is disabled  
✅ Day after check-in and beyond are selectable  
✅ Selected date appears in check-out field  

**Pass/Fail:** ___________

---

### Test Case 3.4: Check-out Before Check-in
**Objective:** Verify check-out date picker enforces minimum date

**Steps:**
1. Select check-in date (e.g., Jan 15)
2. Tap check-out field
3. Try to select Jan 14 or Jan 15

**Expected Results:**
✅ Dates before check-in are grayed out/disabled  
✅ Check-in date itself is disabled  
✅ Cannot select invalid dates  

**Pass/Fail:** ___________

---

### Test Case 3.5: Guest Count Management
**Objective:** Verify guest counter works correctly

**Steps:**
1. On Create Reservation screen, observe initial guest count
2. Tap "+" button twice
3. Observe count
4. Tap "-" button once
5. Observe count

**Expected Results:**
✅ Initial count is 1  
✅ After tapping "+": count increases to 2, then 3  
✅ After tapping "-": count decreases to 2  
✅ Count updates immediately  

**Pass/Fail:** ___________

---

### Test Case 3.6: Guest Count Limits
**Objective:** Verify guest count respects min/max limits

**Steps:**
1. Tap "-" button until count is 1
2. Try to tap "-" again
3. Observe behavior
4. Tap "+" button repeatedly until reaching max guests (4 for Beach House)
5. Try to tap "+" again

**Expected Results:**
✅ Cannot decrease below 1 guest  
✅ Cannot increase beyond max guests (4)  
✅ Toast message appears: "Maximum 4 guests allowed"  

**Pass/Fail:** ___________

---

### Test Case 3.7: Price Calculation
**Objective:** Verify price is calculated correctly

**Prerequisites:** 
- Check-in: Tomorrow
- Check-out: 3 days after check-in (= 3 nights)
- Guests: 2
- Property: Beach House (₱3500/night)

**Expected Results:**
✅ "3 nights" is displayed  
✅ Subtotal: "₱10500" (3 × 3500)  
✅ Total: "₱10500"  
✅ Price updates automatically when dates change  

**Pass/Fail:** ___________

---

### Test Case 3.8: Price Calculation - Different Dates
**Objective:** Verify price recalculates when dates change

**Steps:**
1. Set check-in to tomorrow
2. Set check-out to 2 days later (1 night)
3. Observe price
4. Change check-out to 6 days later (5 nights)
5. Observe price

**Expected Results:**
✅ With 1 night: "1 night" displayed, Total: "₱3500"  
✅ With 5 nights: "5 nights" displayed, Total: "₱17500"  
✅ Price updates immediately after date change  

**Pass/Fail:** ___________

---

### Test Case 3.9: Create Reservation - Success
**Objective:** Verify reservation is created successfully

**Prerequisites:** User is logged in

**Steps:**
1. On Create Reservation screen:
   - Select check-in: Tomorrow
   - Select check-out: 3 days later
   - Set guests: 2
2. Tap "Confirm Reservation" button
3. Wait for processing
4. Observe result

**Expected Results:**
✅ Loading indicator appears on button  
✅ Button is disabled during processing  
✅ Toast message: "Reservation created successfully!"  
✅ App navigates back to previous screen  
✅ No error message  

**Verification:**
- Check Firebase Console → `reservations` collection
- New document should exist with:
  - listingId: [Beach House ID]
  - guestId: [Your user ID]
  - checkInDate: [Selected date]
  - checkOutDate: [Selected date]
  - numberOfGuests: 2
  - totalPrice: 10500
  - status: "pending"

**Pass/Fail:** ___________

---

### Test Case 3.10: Create Reservation - Validation
**Objective:** Verify form validation works

**Steps:**
1. On Create Reservation screen, leave dates empty
2. Tap "Confirm Reservation"
3. Observe result

**Expected Results:**
✅ Toast message: "Please select check-in and check-out dates"  
✅ Reservation is NOT created  
✅ User remains on screen  

**Steps (continued):**
4. Select check-in date only
5. Tap "Confirm Reservation"

**Expected Results:**
✅ Toast message: "Please select check-in and check-out dates"  
✅ Reservation is NOT created  

**Pass/Fail:** ___________

---

### Test Case 3.11: Duplicate Reservation Check
**Objective:** Verify duplicate reservation prevention

**Prerequisites:** Already created a reservation for Beach House

**Steps:**
1. Navigate back to Beach House detail
2. Tap "Reserve" again
3. Select dates
4. Tap "Confirm Reservation"

**Expected Results:**
✅ Toast message: "You already have an active reservation for this property"  
✅ Reservation is NOT created  
✅ User remains on screen  

**Pass/Fail:** ___________

---

### Test Case 3.12: Back Navigation from Reservation
**Objective:** Verify back button works on reservation screen

**Steps:**
1. On Create Reservation screen, tap back button
2. Observe navigation

**Expected Results:**
✅ App returns to Listing Detail screen  
✅ No crash  
✅ No reservation created  

**Pass/Fail:** ___________

---

## 📱 TEST SUITE 4: INTEGRATION TESTS

### Test Case 4.1: Complete User Flow
**Objective:** Verify entire flow from explore to reservation

**Steps:**
1. Open app → Navigate to Explore
2. Search for "Villa"
3. Tap "Mountain View Villa"
4. Review details
5. Tap "Reserve"
6. Select check-in: Tomorrow
7. Select check-out: 2 days later
8. Set guests: 4
9. Verify price: ₱10000 (2 nights × ₱5000)
10. Tap "Confirm Reservation"

**Expected Results:**
✅ Each step works smoothly  
✅ No crashes or errors  
✅ Reservation created successfully  
✅ Data saved to Firestore  

**Pass/Fail:** ___________

---

### Test Case 4.2: Multiple Listings Navigation
**Objective:** Verify navigation between multiple listings

**Steps:**
1. On Explore screen, tap "Beach House"
2. View details, tap back
3. Tap "City Apartment"
4. View details, tap back
5. Tap "Villa"
6. View details

**Expected Results:**
✅ Each navigation works correctly  
✅ Correct listing details shown each time  
✅ No data mixing between listings  
✅ No crashes  

**Pass/Fail:** ___________

---

### Test Case 4.3: Filter and Navigate
**Objective:** Verify filters work with navigation

**Steps:**
1. On Explore screen, set max price: ₱3000
2. Only City Apartment should show
3. Tap City Apartment
4. View details
5. Tap back
6. Clear filters
7. All listings should show again

**Expected Results:**
✅ Filters persist during navigation  
✅ Filters can be cleared after returning  
✅ Listing list updates correctly  

**Pass/Fail:** ___________

---

## 📱 TEST SUITE 5: ERROR HANDLING

### Test Case 5.1: No Internet Connection
**Objective:** Verify app handles offline state gracefully

**Steps:**
1. Turn off WiFi and mobile data
2. Open app
3. Navigate to Explore
4. Observe behavior

**Expected Results:**
✅ Loading indicator appears  
✅ Error message or empty state appears  
✅ App doesn't crash  
✅ User can retry when connection restored  

**Pass/Fail:** ___________

---

### Test Case 5.2: Invalid Listing ID
**Objective:** Verify app handles missing listing gracefully

**Steps:**
1. Manually navigate to a non-existent listing (if possible)
2. Or delete a listing from Firebase while viewing it

**Expected Results:**
✅ Error message: "Listing not found"  
✅ App doesn't crash  
✅ User can navigate back  

**Pass/Fail:** ___________

---

## 📱 TEST SUITE 6: UI/UX TESTS

### Test Case 6.1: Screen Orientation
**Objective:** Verify app works in landscape mode

**Steps:**
1. Rotate device to landscape
2. Navigate through Explore → Detail → Reservation
3. Rotate back to portrait

**Expected Results:**
✅ Layouts adjust properly  
✅ No content is cut off  
✅ All functionality works  
✅ No crashes  

**Pass/Fail:** ___________

---

### Test Case 6.2: Scrolling
**Objective:** Verify all screens scroll properly

**Steps:**
1. On Explore screen, scroll through listings
2. On Detail screen, scroll through all content
3. On Reservation screen, scroll through form

**Expected Results:**
✅ Smooth scrolling  
✅ All content is accessible  
✅ No UI glitches  

**Pass/Fail:** ___________

---

### Test Case 6.3: Text Readability
**Objective:** Verify all text is readable

**Expected Results:**
✅ All text has sufficient contrast  
✅ Font sizes are appropriate  
✅ No text is cut off  
✅ Long text wraps properly  

**Pass/Fail:** ___________

---

## 📊 TEST SUMMARY

### Test Results Overview

| Test Suite | Total Tests | Passed | Failed | Notes |
|------------|-------------|--------|--------|-------|
| Suite 1: Explore Screen | 6 | ___ | ___ | |
| Suite 2: Listing Detail | 4 | ___ | ___ | |
| Suite 3: Reservation | 12 | ___ | ___ | |
| Suite 4: Integration | 3 | ___ | ___ | |
| Suite 5: Error Handling | 2 | ___ | ___ | |
| Suite 6: UI/UX | 3 | ___ | ___ | |
| **TOTAL** | **30** | ___ | ___ | |

### Pass Rate: ____%

---

## 🐛 ISSUES FOUND

### Critical Issues
_List any critical bugs that prevent core functionality_

1. 
2. 
3. 

### Major Issues
_List any major bugs that significantly impact user experience_

1. 
2. 
3. 

### Minor Issues
_List any minor bugs or UI glitches_

1. 
2. 
3. 

### Suggestions for Improvement
_List any UX improvements or enhancements_

1. 
2. 
3. 

---

## ✅ SIGN-OFF

**Tester Name:** ___________________  
**Date:** ___________________  
**Overall Status:** ☐ PASS  ☐ FAIL  ☐ PASS WITH ISSUES  

**Comments:**
_____________________________________
_____________________________________
_____________________________________

---

## 📝 NOTES

- If any test fails, note the exact steps to reproduce
- Take screenshots of any issues
- Check Logcat for error messages
- Verify Firebase data after each reservation creation
- Test on both emulator and physical device if possible

**Testing Complete!** 🎉
