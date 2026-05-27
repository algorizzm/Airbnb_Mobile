# 🚀 QUICK REFERENCE GUIDE

## Project Status at a Glance

**Progress:** 44% Complete (4/9 phases done)  
**Status:** 🟢 On Track  
**Compilation:** ✅ Clean (0 errors)  
**Architecture:** ✅ 100% Intact  

---

## 📁 Key Files by Feature

### Explore Listings
- **Adapter:** `app/src/main/java/com/airbnb/ui/explore/adapter/ListingAdapter.kt`
- **ViewModel:** `app/src/main/java/com/airbnb/ui/explore/ExploreViewModel.kt`
- **Fragment:** `app/src/main/java/com/airbnb/ui/explore/ExploreFragment.kt`
- **Layout:** `app/src/main/res/layout/fragment_explore.xml`
- **Card Layout:** `app/src/main/res/layout/item_listing.xml`

### Listing Details
- **ViewModel:** `app/src/main/java/com/airbnb/ui/listings/ListingDetailViewModel.kt`
- **Fragment:** `app/src/main/java/com/airbnb/ui/listings/ListingDetailFragment.kt`
- **Layout:** `app/src/main/res/layout/fragment_listing_detail.xml`

### Reservations
- **Repository:** `app/src/main/java/com/airbnb/data/repository/ReservationRepository.kt`
- **ViewModel:** `app/src/main/java/com/airbnb/ui/reservations/CreateReservationViewModel.kt`
- **Fragment:** `app/src/main/java/com/airbnb/ui/reservations/CreateReservationFragment.kt`
- **Layout:** `app/src/main/res/layout/fragment_create_reservation.xml`

### Data Models
- **Listing:** `app/src/main/java/com/airbnb/data/model/Listing.kt`
- **Reservation:** `app/src/main/java/com/airbnb/data/model/Reservation.kt`
- **User:** `app/src/main/java/com/airbnb/data/model/User.kt`

### Repositories
- **Listing:** `app/src/main/java/com/airbnb/data/repository/ListingRepository.kt`
- **Reservation:** `app/src/main/java/com/airbnb/data/repository/ReservationRepository.kt`
- **Booking (old):** `app/src/main/java/com/airbnb/data/repository/BookingRepository.kt`

### Navigation
- **Main Graph:** `app/src/main/res/navigation/main_graph.xml`
- **Nav Graph:** `app/src/main/res/navigation/nav_graph.xml`

---

## 🔄 Navigation Flow

```
ExploreFragment
    ↓ (click listing card)
ListingDetailFragment
    ↓ (click reserve button)
CreateReservationFragment
    ↓ (confirm reservation)
Back to previous screen
```

---

## 🔥 Firebase Collections

### Active Collections
- `users/` - User profiles
- `listings/` - Property listings
- `reservations/` - User reservations
- `wishlists/` - User wishlists

### Legacy Collections (Still Active)
- `bookings/` - Hiking bookings
- `hikes/` - Hiking events

---

## 📋 Common Tasks

### Adding a New Screen
1. Create Fragment class
2. Create ViewModel class
3. Create XML layout
4. Add to navigation graph
5. Add navigation action
6. Test navigation flow

### Adding a New Repository Method
1. Add method to Repository class
2. Use Flow for real-time updates
3. Use suspend for one-time operations
4. Return Result<T> for error handling
5. Test with Firestore

### Updating UI
1. Add StateFlow to ViewModel
2. Observe in Fragment
3. Update UI in collect block
4. Handle loading/error states

---

## 🐛 Common Issues & Solutions

### Issue: Unresolved Reference
**Solution:** Check imports, rebuild project, sync Gradle

### Issue: Navigation Action Not Found
**Solution:** Check navigation graph, verify fragment IDs, rebuild

### Issue: ViewBinding Not Generated
**Solution:** Clean project, rebuild, check XML syntax

### Issue: Firestore Not Updating
**Solution:** Check collection name, verify document ID, check permissions

---

## 📚 Documentation Files

### Phase Summaries
- `PHASE2_COMPLETION_SUMMARY.md` - Explore migration
- `PHASE3_COMPLETION_SUMMARY.md` - Listing details
- `PHASE4_COMPLETION_SUMMARY.md` - Reservations

### Progress Tracking
- `documentation/REFACTORING_PROGRESS.md` - Overall status
- `SESSION_SUMMARY.md` - This session's work

### Implementation Guide
- `ImplementationPlan.md` - Original plan

### Bugfixes
- `BUGFIX_ARG_HIKE_ID.md` - Backward compatibility fix

---

## 🎯 Next Steps

### Immediate (Phase 5)
1. Implement wishlist functionality
2. Add wishlist buttons to UI
3. Test wishlist syncing

### Short-term
1. Update Trips screen
2. Display reservations
3. Implement cancellation

### Medium-term
1. Hosting features (Phase 6)
2. Profile updates (Phase 7)
3. Placeholders (Phase 8)

### Long-term
1. Cleanup & testing (Phase 9)
2. Archive old files
3. Final polish

---

## 🔧 Development Commands

### Build Project
```bash
.\gradlew assembleDebug
```

### Clean Build
```bash
.\gradlew clean
.\gradlew assembleDebug
```

### Check for Errors
Use IDE diagnostics or:
```bash
.\gradlew compileDebugKotlin
```

---

## 📞 Key Constants

### Navigation Arguments
- `ARG_LISTING_ID` - Listing identifier
- `ARG_HIKE_ID` - Hike identifier (legacy)

### Reservation Status
- `pending` - Awaiting confirmation
- `confirmed` - Confirmed by host
- `cancelled` - Cancelled
- `completed` - Stay completed

### Payment Status
- `unpaid` - Not paid
- `paid` - Payment received
- `refunded` - Payment refunded

---

## 🎨 UI Components

### Reusable Drawables
- `bg_dark_card` - Dark card background
- `bg_edittext_border` - Input field border
- `bg_button_primary` - Primary button
- `bg_translucent_circle` - Icon button background

### Icons
- `ic_back` - Back arrow
- `ic_calendar` - Calendar icon
- `ic_minus` - Decrease icon
- `ic_plus` - Increase icon
- `ic_notification` - Notification bell
- `ic_settings` - Settings gear

### Fonts
- `poppins_bold` - Bold text
- `poppins_medium` - Medium weight
- `poppins_regular` - Regular text

---

## 💡 Best Practices

### Code Style
✅ Use ViewBinding (no findViewById)  
✅ Use StateFlow for state management  
✅ Use Flow for Firestore streams  
✅ Handle null safely  
✅ Proper error handling  

### Architecture
✅ Follow MVVM pattern  
✅ Keep ViewModels lightweight  
✅ Use Repository for data access  
✅ Lifecycle-aware observers  
✅ Separate concerns  

### Firebase
✅ Use real-time listeners (Flow)  
✅ Handle errors gracefully  
✅ Null-safe field access  
✅ Use Result<T> for operations  
✅ Cache data when appropriate  

### Navigation
✅ Use Navigation Component  
✅ Pass arguments via Bundle  
✅ Handle back navigation  
✅ Catch navigation exceptions  
✅ Test all flows  

---

## 🚨 Important Notes

### DO NOT
❌ Delete old hiking files yet  
❌ Mass rename packages  
❌ Rewrite architecture  
❌ Skip testing  
❌ Ignore compilation errors  

### DO
✅ Refactor incrementally  
✅ Test after each change  
✅ Document changes  
✅ Preserve architecture  
✅ Maintain compatibility  

---

## 📊 Progress Checklist

### Completed ✅
- [x] Phase 1 - Foundation
- [x] Phase 2 - Explore Experience
- [x] Phase 3 - Listing Details
- [x] Phase 4 - Reservations (Creation)

### In Progress 🔄
- [ ] Phase 4 - Trips Screen Update

### Pending ⏳
- [ ] Phase 5 - Wishlists
- [ ] Phase 6 - Hosting
- [ ] Phase 7 - Profile Updates
- [ ] Phase 8 - Placeholders
- [ ] Phase 9 - Cleanup & Testing

---

**Last Updated:** Current Session  
**Quick Reference Version:** 1.0  
**Status:** ✅ Current
