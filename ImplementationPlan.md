# 🏔️ → 🏠 VERDANT TO AIRBNB MVP - REFACTORING IMPLEMENTATION PLAN

---

# 📊 EXECUTIVE SUMMARY

**Current State:**  
Fully functional hiking event reservation app with MVVM architecture, Firebase integration, and robust booking system.

**Target State:**  
Airbnb-inspired property rental MVP maintaining existing architecture and reusing ~70% of backend logic.

**Estimated Effort:**  
Medium complexity — primarily domain model transformation with UI updates.

---

# 🔍 PHASE 1: CODEBASE ANALYSIS RESULTS

## ✅ HIGHLY REUSABLE COMPONENTS (Keep & Refactor)

### 1. Architecture & Infrastructure (95% Reusable)

- ✅ MVVM architecture pattern
- ✅ ViewBinding implementation
- ✅ Navigation Component setup
- ✅ Firebase Auth integration (`FirebaseAuthService.kt`)
- ✅ Firebase Firestore service (`FirestoreService.kt`)
- ✅ Firebase Storage service (`StorageService.kt`)
- ✅ Coroutines & Flow implementation
- ✅ Repository pattern
- ✅ AuthManager & session management

---

### 2. Core Business Logic (80% Reusable)

#### ✅ Booking System — Nearly identical to reservation system

- `BookingRepository.kt` → Rename to `ReservationRepository.kt`
- Booking status management (`pending`, `approved`, `cancelled`, `completed`)
- User-to-listing relationship tracking

#### ✅ User Management

- `UserRepository.kt` — Fully reusable
- `User.kt` model — Minor field updates needed
- Profile management (avatar, banner, bio)
- Role-based permissions system

#### ✅ Authentication Flow

- `AuthRepository.kt` — Fully reusable
- Login/Signup fragments
- Guest mode support
- Protected navigation

---

### 3. UI Components (60% Reusable)

- ✅ Bottom navigation structure
- ✅ RecyclerView adapters pattern
- ✅ Fragment navigation
- ✅ Dialog components (`GuestPromptDialog`, `EditTextDialog`)
- ✅ Avatar helper utilities
- ✅ Image upload functionality
- ✅ Search & filter UI patterns

---

### 4. Utilities & Helpers (90% Reusable)

- ✅ Permission handling
- ✅ Status management pattern
- ✅ Image loading (Glide)
- ✅ Date/time handling
- ✅ Toast extensions

---

# 🔄 COMPONENTS REQUIRING TRANSFORMATION

## 1. Domain Models (Rename & Refactor)

| Current (Hiking) | Target (Airbnb) | Transformation |
|---|---|---|
| `Hike.kt` | `Listing.kt` | Rename fields, keep structure |
| `Booking.kt` | `Reservation.kt` | Minimal changes |
| `User.kt` (role: guide/hiker) | `User.kt` (role: host/guest) | Update role constants |
| `HikePost.kt` | Remove/Archive | Not needed for MVP |
| `FeedItem.kt` | Remove/Archive | Not needed for MVP |

---

## 2. Repositories (Rename & Update)

| Current | Target | Changes |
|---|---|---|
| `HikeRepository.kt` | `ListingRepository.kt` | Collection name, field mappings |
| `BookingRepository.kt` | `ReservationRepository.kt` | Collection name, terminology |
| `PostRepository.kt` | Archive | Not needed for MVP |

---

## 3. UI Screens (Refactor & Rename)

| Current | Target | Complexity |
|---|---|---|
| `ExploreFragment` (hikes) | `ExploreFragment` (properties) | Low — adapter changes |
| `HikeDetailFragment` | `ListingDetailFragment` | Medium — field mappings |
| `HikesFragment` (create) | `HostListingsFragment` | Medium — terminology |
| `CreateHikeFlow` (7 steps) | `CreateListingFlow` (simplified) | High — reduce steps |
| `HomeFragment` (feed) | Archive/Simplify | Low priority for MVP |
| `ProfileFragment` | `ProfileFragment` | Low — minor updates |
| `MessagesFragment` | Keep as placeholder | No changes |

---

# ❌ OBSOLETE COMPONENTS (Archive/Remove)

## Hiking-Specific Logic

- ❌ `HikePost.kt`
- ❌ `CompletionPost.kt`
- ❌ `FeedItem.kt`
- ❌ `PostRepository.kt`
- ❌ `WeatherRepository.kt`
- ❌ Trail tracking features
- ❌ Hike start/complete/ongoing status logic
- ❌ Participant tracking during hikes
- ❌ Distance/elevation/summit statistics

---

## UI Components to Remove

- ❌ `HomeFragment`
- ❌ `PostDetailFragment`
- ❌ `EventsFragment`
- ❌ Weather-related UI components
- ❌ Hike tracking floating cards

---

# 🗺️ PHASE 2: FIREBASE COLLECTION MAPPING

```text
CURRENT (Verdant)          →    TARGET (Airbnb MVP)
─────────────────────────────────────────────────────

users/                     →    users/
  - role: "guide"/"hiker"       - role: "host"/"guest"
  - totalHikes                  - (remove hiking stats)
  - totalDistance               
  - totalSummits                

hikes/                     →    listings/
  - title                       - title
  - description                 - description
  - location/meetupPoint        - location/address
  - difficulty                  - (remove)
  - distanceKm                  - (remove)
  - elevationM                  - (remove)
  - price                       - pricePerNight
  - guideId                     - hostId
  - guideName                   - hostName
  - maxParticipants             - maxGuests
  - imageUrl                    - imageUrl
  - galleryImageUrls            - galleryImageUrls
  - inclusions                  - amenities
  - requirements                - houseRules
  - startDateTime               - (remove)
  - endDateTime                 - (remove)

bookings/                  →    reservations/
  - hikeId                      - listingId
  - userId                      - guestId
  - userName                    - guestName
  - guideId                     - hostId
  - status                      - status
  - paymentStatus               - paymentStatus
  - createdAt                   - createdAt
                                + checkInDate
                                + checkOutDate
                                + totalPrice
                                + numberOfGuests

(NEW)                      →    wishlists/
                                - userId
                                - listingIds: []
                                - createdAt

posts/                     →    (ARCHIVE - not in MVP)
messages/                  →    (PLACEHOLDER - future)
notifications/             →    notifications/ (keep)
```

---

# 📋 PHASE 3: DETAILED IMPLEMENTATION ROADMAP

---

# STAGE 1: Foundation & Data Models
**Priority:** CRITICAL  
**Estimated Time:** 2–3 hours

## 1.1 Update Constants & Enums

### File: `Permissions.kt`

#### BEFORE

```kotlin
object UserRole {
    const val GUEST = "guest"
    const val HIKER = "hiker"
    const val GUIDE = "guide"
    const val ADMIN = "admin"
}

object HikeStatus {
    const val DRAFT = "draft"
    const val OPEN = "open"
    const val FULL = "full"
    const val ONGOING = "ongoing"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
}
```

#### AFTER

```kotlin
object UserRole {
    const val GUEST = "guest"
    const val HOST = "host"
    const val ADMIN = "admin"
}

object ListingStatus {
    const val DRAFT = "draft"
    const val ACTIVE = "active"
    const val INACTIVE = "inactive"
    const val CANCELLED = "cancelled"
}

object ReservationStatus {
    const val PENDING = "pending"
    const val CONFIRMED = "confirmed"
    const val REJECTED = "rejected"
    const val CANCELLED = "cancelled"
    const val COMPLETED = "completed"
}
```

---

## 1.2 Create New Data Models

### New File: `Listing.kt`

```kotlin
data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val propertyType: String = "",
    val pricePerNight: Double = 0.0,
    val hostId: String = "",
    val hostName: String = "",
    val maxGuests: Int = 1,
    val bedrooms: Int = 1,
    val beds: Int = 1,
    val bathrooms: Int = 1,
    val status: String = ListingStatus.ACTIVE,
    val imageUrl: String = "",
    val galleryImageUrls: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val houseRules: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
```

---

### New File: `Reservation.kt`

```kotlin
data class Reservation(
    val id: String = "",
    val listingId: String = "",
    val guestId: String = "",
    val guestName: String = "",
    val hostId: String = "",
    val checkInDate: Timestamp? = null,
    val checkOutDate: Timestamp? = null,
    val numberOfGuests: Int = 1,
    val totalPrice: Double = 0.0,
    val status: String = ReservationStatus.PENDING,
    val paymentStatus: String = PaymentStatus.UNPAID,
    val createdAt: Timestamp? = null
)
```

---

### New File: `Wishlist.kt`

```kotlin
data class Wishlist(
    val id: String = "",
    val userId: String = "",
    val listingIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null
)
```

---

## 1.3 Update User Model

### File: `User.kt`

```kotlin
data class User(
    val id: String = "",
    val fname: String = "",
    val lname: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "guest",
    val location: String = "",
    val profileImage: String = "",
    val bannerImage: String = "",
    val bio: String? = null
)
```

---

# STAGE 2: Repository Layer
**Priority:** CRITICAL  
**Estimated Time:** 3–4 hours

## 2.1 Create ListingRepository

### Action

Copy:

```text
HikeRepository.kt → ListingRepository.kt
```

### Key Changes

- Collection: `"hikes"` → `"listings"`
- Remove:
  - `startHike()`
  - `completeHike()`
  - `hasOngoingHike()`
- Update field mappings in `toFirestoreMap()`
- Keep:
  - CRUD operations
  - Observe flows
  - Status updates

---

## 2.2 Create ReservationRepository

### Action

Copy:

```text
BookingRepository.kt → ReservationRepository.kt
```

### Key Changes

- Collection: `"bookings"` → `"reservations"`
- Rename:
  - `hikeId` → `listingId`
  - `userId` → `guestId`
  - `guideId` → `hostId`
- Update:
  - `findActiveBookingForUserOnHike()`
  → `findActiveReservationForListing()`

---

## 2.3 Create WishlistRepository

### New File: `WishlistRepository.kt`

```kotlin
class WishlistRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val wishlistsCol get() = db.collection("wishlists")

    fun observeWishlist(userId: String): Flow<List<String>> = callbackFlow {
        val reg = wishlistsCol.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val wishlist = snapshot?.toObject(Wishlist::class.java)
                trySend(wishlist?.listingIds ?: emptyList())
            }

        awaitClose { reg.remove() }
    }

    suspend fun addToWishlist(
        userId: String,
        listingId: String
    ): Result<Unit> = runCatching {
        wishlistsCol.document(userId).set(
            mapOf("listingIds" to FieldValue.arrayUnion(listingId)),
            SetOptions.merge()
        ).await()
    }

    suspend fun removeFromWishlist(
        userId: String,
        listingId: String
    ): Result<Unit> = runCatching {
        wishlistsCol.document(userId).update(
            "listingIds",
            FieldValue.arrayRemove(listingId)
        ).await()
    }
}
```

---

# STAGE 3: UI Layer - Explore & Listings
**Priority:** HIGH  
**Estimated Time:** 4–5 hours

## 3.1 Update ExploreFragment & ViewModel

### Files

- `ExploreFragment.kt`
- `ExploreViewModel.kt`
- `HikeAdapter.kt` → `ListingAdapter.kt`

### Changes

- Replace `HikeRepository` with `ListingRepository`
- Update adapter to display property cards
- Update filters:
  - Remove difficulty/distance
  - Add property type, price range, guests
- Keep:
  - Search functionality
  - RecyclerView structure

---

## Layout Changes

### `item_hike.xml` → `item_listing.xml`

Update fields:

- Show:
  - Price per night
  - Bedrooms
  - Location
- Remove:
  - Difficulty badge
  - Distance
  - Elevation

---

# STAGE 4: Wishlist Feature
**Priority:** HIGH  
**Estimated Time:** 3–4 hours

## 4.1 Create WishlistFragment

```kotlin
class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private val viewModel: WishlistViewModel by viewModels()

    private val adapter = ListingAdapter { listing ->
        // Navigate to detail
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup RecyclerView
        // Observe wishlist
    }
}
```

---

## 4.2 Create WishlistViewModel

```kotlin
class WishlistViewModel : ViewModel() {

    private val wishlistRepo = WishlistRepository()
    private val listingRepo = ListingRepository()

    val wishlistedListings: StateFlow<List<Listing>> = // Combine flows

    fun toggleWishlist(listingId: String) { }
}
```

---

# STAGE 5: Trips/Reservations
**Priority:** HIGH  
**Estimated Time:** 3–4 hours

## 5.1 Create TripsFragment

### Action

Refactor:

```text
HikeHistoryFragment.kt → TripsFragment.kt
```

### Changes

- Show user's reservations:
  - Upcoming
  - Past
  - Cancelled
- Add tabs:
  - Upcoming
  - Past
  - Cancelled

---

## 5.2 Update Booking Flow

### Action

Refactor:

```text
BookingFragment.kt → ReservationFragment.kt
```

### Changes

- Add date picker for check-in/check-out
- Add guest count selector
- Calculate total price based on nights
- Keep:
  - Payment status
  - Confirmation flow

---

# STAGE 6: Host Features
**Priority:** MEDIUM  
**Estimated Time:** 5–6 hours

## 6.1 Simplify Create Listing Flow

### Current Steps

1. Host Info
2. Route Details
3. Schedule
4. Details
5. Media
6. Pricing
7. Review

### Target Steps

1. Basic Info
2. Details
3. Photos
4. Pricing & Review

---

## Remove

- `CreateHikeRouteFragment`
- `CreateHikeScheduleFragment`

---

# STAGE 7: Profile & Settings
**Priority:** LOW  
**Estimated Time:** 2–3 hours

## 7.1 Update ProfileFragment

### Changes

- Remove hiking stats
- Update:
  - `"Recent Hikes"` → `"Recent Trips"`
- Add:
  - `"Switch to Host"` button

---

# STAGE 8: Messages Placeholder
**Priority:** LOW  
**Estimated Time:** 1 hour

## 8.1 Keep MessagesFragment as Placeholder

### Changes

- Show `"Coming Soon"` message
- Placeholder for host-guest conversations

---

# STAGE 9: Cleanup & Polish
**Priority:** MEDIUM  
**Estimated Time:** 3–4 hours

## 9.1 Archive Obsolete Files

Move to `_archive/` folder:

- `HikePost.kt`
- `CompletionPost.kt`
- `FeedItem.kt`
- `PostRepository.kt`
- `WeatherRepository.kt`
- `HomeFragment.kt`
- `PostDetailFragment.kt`
- `EventsFragment.kt`

---

## 9.2 Update Strings & Resources

### `strings.xml`

- `"Verdant"` → `"Airbnb Clone"` or custom name
- Replace hiking terminology

---

## 9.3 Update AndroidManifest

```xml
<application
    android:name=".AirbnbApplication"
    android:label="@string/app_name"
    android:theme="@style/Theme.Airbnb">
```

---

# STAGE 10: Testing & Verification
**Priority:** CRITICAL  
**Estimated Time:** 2–3 hours

## 10.1 Firebase Setup

- Create collections:
  - `listings`
  - `reservations`
  - `wishlists`
- Migrate sample data
- Update Firestore security rules

---

## 10.2 Manual Testing Checklist

- Authentication
- Explore listings
- Listing details
- Create reservation
- Wishlist add/remove
- View trips
- Host create listing
- Host manage reservations
- Profile editing
- Navigation flow

---

# 📊 PHASE 4: PACKAGE RESTRUCTURING

## Current Structure

```text
com.airbnb/
├── core/
├── data/
├── ui/
│   ├── hikes/
│   ├── home/
│   ├── profile/
│   └── settings/
└── utils/
```

---

## Target Structure

```text
com.airbnb/
├── core/
├── data/
│   ├── model/
│   │   ├── Listing.kt
│   │   ├── Reservation.kt
│   │   ├── Wishlist.kt
│   │   ├── User.kt
│   │   └── _archive/
│   ├── repository/
│   │   ├── ListingRepository.kt
│   │   ├── ReservationRepository.kt
│   │   ├── WishlistRepository.kt
│   │   └── _archive/
│   └── remote/
├── ui/
│   ├── auth/
│   ├── explore/
│   ├── listings/
│   ├── trips/
│   ├── wishlist/
│   ├── messages/
│   ├── profile/
│   └── _archive/
└── utils/
```

---

# ⚠️ RISK MITIGATION & BEST PRACTICES

## Incremental Migration Strategy

- ✅ Keep old models/repos temporarily
- ✅ Use feature flags
- ✅ Test each stage independently

---

## Data Migration

- ✅ Don't delete Firestore collections immediately
- ✅ Create migration scripts
- ✅ Maintain backward compatibility

---

## Testing Strategy

- ✅ Test authentication flow first
- ✅ Test CRUD operations
- ✅ Test navigation
- ✅ Test guest and host roles

---

## Rollback Plan

- ✅ Keep `_archive/` folders
- ✅ Use Git branches
- ✅ Document breaking changes

---

# 📈 COMPLEXITY ESTIMATES

| Stage | Complexity | Time | Priority |
|---|---|---|---|
| 1. Data Models | Low | 2–3h | CRITICAL |
| 2. Repositories | Medium | 3–4h | CRITICAL |
| 3. Explore UI | Medium | 4–5h | HIGH |
| 4. Wishlist | Low | 3–4h | HIGH |
| 5. Trips | Medium | 3–4h | HIGH |
| 6. Host Features | High | 5–6h | MEDIUM |
| 7. Profile | Low | 2–3h | LOW |
| 8. Messages | Low | 1h | LOW |
| 9. Cleanup | Low | 3–4h | MEDIUM |
| 10. Testing | Medium | 2–3h | CRITICAL |

---

# 🎯 MVP FEATURE CHECKLIST

## Must Have (P0)

- Authentication
- Explore listings
- Listing details
- Create reservation
- View trips
- Wishlist
- Host create listing
- Host manage reservations
- Profile management

---

## Should Have (P1)

- Advanced search filters
- Date range picker
- Host dashboard analytics
- Reservation cancellation flow
- Email notifications

---

## Nice to Have (P2)

- Real-time messaging
- Reviews & ratings
- Map view
- Payment integration
- Multi-language support

---

# 🚀 RECOMMENDED EXECUTION ORDER

## Week 1: Foundation

- Stage 1
- Stage 2
- Stage 10 Firebase setup

---

## Week 2: Core Features

- Stage 3
- Stage 4
- Stage 5

---

## Week 3: Host Features

- Stage 6
- Stage 7

---

## Week 4: Polish

- Stage 9
- Stage 10 testing
- Bug fixes

---

# 📝 NEXT STEPS

Awaiting approval to proceed with:

- ✅ Stage 1: Data Models
- ✅ Stage 2: Repositories
- ✅ Incremental testing

---

## Options

### A) Start with Stage 1
Data Models & Constants

### B) Provide more detail
Specific stage deep dive

### C) Adjust priorities or scope

### D) Create a different implementation approach