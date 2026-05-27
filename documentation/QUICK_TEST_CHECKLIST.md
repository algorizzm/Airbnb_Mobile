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

**Status:**     

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

**Status:** ✅ PASS  ☐ FAIL

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

**Status:** ✅ PASS  ☐ FAIL

---

## ❤️ WISHLIST (5 min)

### Add to Wishlist
- [ ] Tap heart icon on listing card
- [ ] Heart fills in
- [ ] Toast: "Added to wishlist"
- [ ] Check Firestore → Wishlist updated

### Remove from Wishlist
- [ ] Tap filled heart icon
- [ ] Heart empties
- [ ] Toast: "Removed from wishlist"
- [ ] Check Firestore → Wishlist updated

### Wishlist Screen
- [ ] Navigate to Wishlists tab
- [ ] Wishlisted listings appear
- [ ] Tap listing → Opens detail screen
- [ ] Tap heart → Removes from wishlist
- [ ] Empty state shows when no wishlists

**Status:** ✅ PASS  ☐ FAIL

---

## 🗓️ TRIPS SCREEN (10 min)

### Display
- [ ] Navigate to Trips tab
- [ ] Filter buttons visible (Upcoming/Past/Cancelled)
- [ ] Upcoming filter active by default
- [ ] Trips display correctly

### Trip Cards
- [ ] Listing image shows
- [ ] Title and location visible
- [ ] Check-in/check-out dates formatted correctly
- [ ] Status shows with correct color:
  - [ ] Green for upcoming/confirmed
  - [ ] Gray for completed
  - [ ] Red for cancelled
- [ ] Total price displays
- [ ] Cancel button shows only for upcoming trips

### Filtering
- [ ] Tap "Upcoming" → Shows active reservations
- [ ] Tap "Past" → Shows completed reservations
- [ ] Tap "Cancelled" → Shows cancelled reservations
- [ ] Filter button highlights correctly
- [ ] Empty state shows when no trips in filter

### Cancellation
- [ ] Tap "Cancel Reservation" on upcoming trip
- [ ] Confirmation dialog appears
- [ ] Tap "No" → Dialog dismisses, no change
- [ ] Tap "Yes" → Reservation cancelled
- [ ] Toast: "Reservation cancelled successfully"
- [ ] Trip moves to Cancelled filter
- [ ] Check Firestore → Status updated to "cancelled"

### Navigation
- [ ] Tap trip card → Opens listing detail
- [ ] Back button → Returns to Trips

### Real-time Sync
- [ ] Create new reservation from another device/browser
- [ ] New trip appears automatically in Trips screen
- [ ] Cancel reservation from another device/browser
- [ ] Trip updates automatically

**Status:** ☐ PASS  ☐ FAIL

---

## 🔄 INTEGRATION (5 min)

### Complete Flow
- [ ] Explore → Search "Villa"
- [ ] Tap Villa → View details
- [ ] Tap heart → Add to wishlist
- [ ] Tap Reserve → Create reservation
- [ ] Select dates, guests
- [ ] Confirm → Success
- [ ] Navigate to Trips → Reservation appears
- [ ] Navigate to Wishlists → Villa appears
- [ ] Check Firestore → All data saved

### Multiple Listings
- [ ] Navigate to 3 different listings
- [ ] Each shows correct data
- [ ] No data mixing

### Bottom Navigation
- [ ] Tap Explore → Explore screen
- [ ] Tap Wishlists → Wishlist screen
- [ ] Tap Trips → Trips screen
- [ ] Tap Messages → Messages screen
- [ ] Tap Profile → Profile screen
- [ ] All tabs work correctly

**Status:** ✅ PASS  ☐ FAIL

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

**Status:** ☐ PASS  ✅ FAIL

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

**Total Time:** ~50 minutes  
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
- [ ] Cancelled reservation has status: "cancelled"

### Firestore → `wishlists` collection
- [ ] User wishlist document exists
- [ ] listingIds array contains wishlisted items

**Firebase Status:** ☐ VERIFIED  ☐ ISSUES FOUND

---

**Testing Complete!** ✅
