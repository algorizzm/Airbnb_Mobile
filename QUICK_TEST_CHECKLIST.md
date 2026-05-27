# ✅ QUICK TEST CHECKLIST

## Pre-Test Setup
- [ ] Add 3 sample listings to Firestore
- [ ] Build and install app: `.\gradlew installDebug`
- [ ] Launch app and log in

---

## 🔍 EXPLORE SCREEN (5 min)

### Basic Display
- [ ] Listings appear on Explore tab
- [ ] Each card shows: title, location, guests, price
- [ ] Cards are scrollable

### Search
- [ ] Search "Beach" → Shows Beach House only
- [ ] Search "Manila" → Shows City Apartment only
- [ ] Search "xyz" → Shows empty state

### Filters
- [ ] Max price ₱3000 → Shows only City Apartment
- [ ] Min guests 5 → Shows only Villa
- [ ] Clear filters → Shows all listings

### Navigation
- [ ] Tap listing card → Opens detail screen
- [ ] Back button → Returns to Explore

**Status:** ☐ PASS  ☐ FAIL

---

## 📋 LISTING DETAIL (3 min)

### Display
- [ ] Title, location, property type visible
- [ ] Guest capacity, bedrooms, bathrooms shown
- [ ] Host name displayed
- [ ] Description visible
- [ ] Amenities list with bullets
- [ ] Price at bottom
- [ ] Reserve button visible

### Navigation
- [ ] Back button → Returns to Explore
- [ ] Reserve button → Opens reservation screen

**Status:** ☐ PASS  ☐ FAIL

---

## 📅 RESERVATION CREATION (10 min)

### Display
- [ ] Listing summary at top
- [ ] Date picker fields visible
- [ ] Guest counter visible
- [ ] Price breakdown visible

### Date Selection
- [ ] Check-in picker opens
- [ ] Can select tomorrow or later
- [ ] Check-out picker opens
- [ ] Can only select after check-in
- [ ] Selected dates display correctly

### Guest Counter
- [ ] Starts at 1
- [ ] "+" increases count
- [ ] "-" decreases count
- [ ] Cannot go below 1
- [ ] Cannot exceed max guests
- [ ] Toast shows when max reached

### Price Calculation
- [ ] Nights calculated correctly
- [ ] Price = nights × price per night
- [ ] Updates when dates change

### Create Reservation
- [ ] Select dates: tomorrow + 3 days later
- [ ] Set guests: 2
- [ ] Tap "Confirm Reservation"
- [ ] Loading indicator appears
- [ ] Success toast appears
- [ ] Navigates back
- [ ] Check Firestore → Reservation exists

### Validation
- [ ] No dates → Error toast
- [ ] Only check-in → Error toast
- [ ] Duplicate reservation → Error toast

### Navigation
- [ ] Back button → Returns to detail

**Status:** ☐ PASS  ☐ FAIL

---

## 🔄 INTEGRATION (5 min)

### Complete Flow
- [ ] Explore → Search "Villa"
- [ ] Tap Villa → View details
- [ ] Tap Reserve → Create reservation
- [ ] Select dates, guests
- [ ] Confirm → Success
- [ ] Check Firestore → Data saved

### Multiple Listings
- [ ] Navigate to 3 different listings
- [ ] Each shows correct data
- [ ] No data mixing

**Status:** ☐ PASS  ☐ FAIL

---

## 🎨 UI/UX (3 min)

### Visual
- [ ] Text is readable
- [ ] Colors are consistent
- [ ] No UI glitches
- [ ] Smooth scrolling

### Responsiveness
- [ ] Rotate to landscape → Works
- [ ] Rotate to portrait → Works
- [ ] All content accessible

**Status:** ☐ PASS  ☐ FAIL

---

## 🐛 ERROR HANDLING (2 min)

### Offline
- [ ] Turn off internet
- [ ] Open Explore
- [ ] Graceful error handling
- [ ] No crash

**Status:** ☐ PASS  ☐ FAIL

---

## 📊 OVERALL RESULT

**Total Time:** ~30 minutes  
**Pass Rate:** ____%  
**Status:** ☐ ALL PASS  ☐ PASS WITH ISSUES  ☐ FAIL  

### Critical Issues Found:
1. _________________________________
2. _________________________________
3. _________________________________

### Notes:
_____________________________________
_____________________________________
_____________________________________

**Tester:** ___________________  
**Date:** ___________________  

---

## 🎯 QUICK FIREBASE VERIFICATION

After testing, verify in Firebase Console:

### Firestore → `listings` collection
- [ ] 3 test listings exist
- [ ] All fields populated correctly

### Firestore → `reservations` collection
- [ ] New reservation document exists
- [ ] Fields match test data:
  - [ ] listingId
  - [ ] guestId
  - [ ] checkInDate
  - [ ] checkOutDate
  - [ ] numberOfGuests
  - [ ] totalPrice
  - [ ] status: "pending"

**Firebase Status:** ☐ VERIFIED  ☐ ISSUES FOUND

---

**Testing Complete!** ✅
