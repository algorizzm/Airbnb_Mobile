# 🏠 VERDANT → AIRBNB MVP

# REFACTORING IMPLEMENTATION PLAN

---

# 📌 PROJECT OVERVIEW

## Current Project

A Kotlin Android hiking reservation application called **Verdant** built using:

* Android Studio Iguana
* Kotlin
* XML Layouts
* MVVM Architecture
* Firebase Authentication
* Firebase Firestore
* Firebase Storage
* Navigation Component
* ViewBinding

---

## Refactor Goal

Transform the existing hiking reservation app into an Airbnb-inspired mobile application MVP while preserving as much existing architecture and backend logic as possible.

The goal is NOT to build a production-grade Airbnb clone.

The goal IS to build:

* a stable,
* visually convincing,
* functional,
* demo-ready Airbnb-style prototype.

---

# 🎯 PRIMARY DEVELOPMENT STRATEGY

## REFACTOR > REWRITE

This project already contains:

* reservation systems,
* CRUD operations,
* Firebase integration,
* user management,
* MVVM architecture,
* navigation flows,
* adapters and repositories.

Approximately **70% of backend/business logic is reusable**.

The project should be transformed incrementally instead of rebuilt from scratch.

---

# 🧠 CORE ARCHITECTURE DECISION

# UNIFIED USER SYSTEM

Instead of separate:

* Guest accounts
* Host accounts

The application will use:

```plaintext
One universal user system
```

Every user can:

* browse listings,
* reserve stays,
* save wishlists,
* and become a host.

Hosting is treated as a feature/mode, not a separate role.

---

# ✅ BENEFITS OF THIS APPROACH

* Simpler Firebase structure
* Simpler authentication flow
* Less UI complexity
* Fewer conditional branches
* Easier refactoring
* More similar to actual Airbnb
* Faster MVP development

---

# 📱 TARGET MVP FEATURES

## Authentication

* Login
* Register
* Firebase Authentication
* Persistent login sessions

---

## Main Navigation (Bottom Navigation)

The app should mirror Airbnb's navigation structure:

1. Explore
2. Wishlists
3. Trips
4. Messages (placeholder only)
5. Profile

---

# MVP FEATURE LIST

## 1. Explore Screen

### Features

* Property listings feed
* RecyclerView/Grid layout
* Search bar
* Simple filtering
* Listing cards
* Listing images

### Keep Simple

* No advanced maps
* No complex filtering
* No recommendation engine

---

## 2. Listing Detail Screen

### Features

* Property images
* Title
* Location
* Price per night
* Amenities
* Description
* Host information
* Reserve button

---

## 3. Reservation System

### Features

* Create reservation
* Select check-in/check-out
* View trips
* Cancel reservation

### Simplifications

* No real availability conflict engine
* No payment gateway
* No calendar synchronization

---

## 4. Wishlist System

### Features

* Save listings
* Remove listings
* View wishlisted properties

### Firebase Structure

```plaintext
wishlists/
  userId/
    listingIds[]
```

Keep implementation lightweight.

---

## 5. Profile Screen

### Features

* Edit profile
* Logout
* Hosting access
* View user information

### Remove

* Hiking statistics
* Summit tracking
* Distance metrics

---

## 6. Hosting Features

### Features

* Create listing
* Edit listing
* Delete listing
* Manage reservations

### Important

Hosting should feel like:

```plaintext
"Enable Hosting"
```

not:

```plaintext
"Create separate host account"
```

---

## 7. Messages Screen

### MVP Scope

Placeholder only.

### UI

* "Coming Soon"
* Empty state illustration/text

No real-time messaging implementation yet.

---

# 🧱 FIREBASE STRUCTURE

## Users

```plaintext
users/
```

Example:

```json
{
  "id": "",
  "name": "",
  "email": "",
  "profileImage": "",
  "bio": "",
  "hostModeEnabled": false,
  "totalTrips": 0,
  "totalListings": 0
}
```

---

## Listings

```plaintext
listings/
```

Example:

```json
{
  "id": "",
  "title": "",
  "description": "",
  "location": "",
  "pricePerNight": 0,
  "hostId": "",
  "imageUrl": "",
  "galleryImageUrls": [],
  "amenities": [],
  "maxGuests": 1,
  "bedrooms": 1,
  "bathrooms": 1,
  "createdAt": ""
}
```

---

## Reservations

```plaintext
reservations/
```

Example:

```json
{
  "id": "",
  "listingId": "",
  "guestId": "",
  "hostId": "",
  "checkInDate": "",
  "checkOutDate": "",
  "numberOfGuests": 1,
  "totalPrice": 0,
  "status": "pending"
}
```

---

## Wishlists

```plaintext
wishlists/
```

Example:

```json
{
  "userId": "",
  "listingIds": []
}
```

---

# 🔄 REUSABLE COMPONENTS

## HIGHLY REUSABLE

### Keep & Refactor

* FirebaseAuthService
* FirestoreService
* StorageService
* MVVM architecture
* Repository pattern
* Navigation Component
* ViewBinding
* RecyclerView adapters
* Booking logic
* User management
* Image upload system
* Coroutines & Flows

---

# ❌ REMOVE / ARCHIVE

## Hiking-Specific Features

Move to `_archive/`

* Hike tracking
* Trail difficulty
* Distance/elevation metrics
* Weather system
* Hiking posts/feed system
* Completion tracking
* Summit statistics
* Route scheduling logic

---

# 🗂️ PACKAGE STRUCTURE

## Target Structure

```plaintext
com.airbnb/
├── core/
├── data/
│   ├── model/
│   ├── repository/
│   ├── remote/
│   └── local/
├── ui/
│   ├── auth/
│   ├── explore/
│   ├── listings/
│   ├── trips/
│   ├── wishlist/
│   ├── messages/
│   ├── profile/
│   ├── hosting/
│   └── _archive/
├── utils/
└── adapters/
```

---

# 🚀 SAFE REFACTOR STRATEGY

# IMPORTANT RULE

DO NOT:

* mass rename entire project immediately
* delete old files early
* rewrite architecture
* switch to Jetpack Compose
* introduce unnecessary libraries

---

# SAFE MIGRATION PROCESS

## STEP 1 — Create New Models

Create:

* Listing.kt
* Reservation.kt
* Wishlist.kt

WITHOUT deleting old hiking models yet.

---

## STEP 2 — Create New Repositories

Create:

* ListingRepository
* ReservationRepository
* WishlistRepository

Keep old repositories temporarily.

---

## STEP 3 — Make Explore Screen Work First

Priority checkpoint:

* listings load,
* RecyclerView works,
* Firebase reads work,
* navigation works.

This verifies architecture stability.

---

## STEP 4 — Listing Details

Once Explore works:

* implement listing details,
* property cards,
* reservation button.

---

## STEP 5 — Reservation System

Refactor existing booking flow:

```plaintext
Booking → Reservation
```

Reuse:

* booking status logic,
* Firebase writes,
* user relationships.

---

## STEP 6 — Wishlist System

Implement lightweight wishlist functionality.

Keep it simple.

---

## STEP 7 — Hosting

Only after guest-side MVP is stable:

* create listing flow,
* edit listing,
* delete listing.

---

# 📋 IMPLEMENTATION PHASES

# PHASE 1 — FOUNDATION

Priority: CRITICAL

### Tasks

* Rename app branding
* Create new models
* Create new repositories
* Setup Firestore collections
* Update constants
* Create archive folders

### Estimated Time

2–3 hours

---

# PHASE 2 — EXPLORE EXPERIENCE

Priority: CRITICAL

### Tasks

* Refactor ExploreFragment
* Create ListingAdapter
* Connect listings collection
* Create listing cards
* Implement search bar

### Estimated Time

4–5 hours

---

# PHASE 3 — LISTING DETAILS

Priority: HIGH

### Tasks

* Create ListingDetailFragment
* Display listing information
* Image gallery support
* Reserve button

### Estimated Time

3–4 hours

---

# PHASE 4 — RESERVATIONS / TRIPS

Priority: HIGH

### Tasks

* Reservation creation
* Trips screen
* Reservation status handling
* Cancellation flow

### Estimated Time

4–5 hours

---

# PHASE 5 — WISHLISTS

Priority: HIGH

### Tasks

* Save/remove wishlist
* Wishlist screen
* Firebase syncing

### Estimated Time

2–3 hours

---

# PHASE 6 — HOSTING

Priority: MEDIUM

### Tasks

* Create listing
* Edit listing
* Delete listing
* Manage reservations

### Simplification

No analytics dashboard.

### Estimated Time

5–6 hours

---

# PHASE 7 — PROFILE & SETTINGS

Priority: MEDIUM

### Tasks

* Update profile UI
* Remove hiking stats
* Add hosting access
* Logout

### Estimated Time

2–3 hours

---

# PHASE 8 — PLACEHOLDER FEATURES

Priority: LOW

### Tasks

* Messages placeholder
* Empty states
* Future feature placeholders

### Estimated Time

1 hour

---

# PHASE 9 — CLEANUP & TESTING

Priority: CRITICAL

### Tasks

* Archive obsolete files
* Fix broken imports
* Verify navigation
* Emulator testing
* Firebase testing
* UI cleanup
* Bug fixing

### Estimated Time

4–6 hours

---

# 🧪 TESTING PRIORITIES

## Test First

1. Authentication
2. Firebase reads/writes
3. Navigation
4. Explore listings
5. Reservation flow

---

## Test Later

* Hosting
* Wishlist
* UI polish

---

# ⚠️ RISK MITIGATION

## ALWAYS

* Commit frequently
* Use Git branches
* Archive before deleting
* Refactor incrementally

---

## NEVER

* Refactor entire project in one prompt
* Let AI rename hundreds of files simultaneously
* Trust generated code blindly
* Skip emulator/device testing

---

# 🤖 AI TOOL USAGE STRATEGY

## Cursor/Kiro SHOULD Handle

* repetitive renaming
* repository conversion
* adapter boilerplate
* ViewModel scaffolding
* Firebase mappings
* XML placeholder generation

---

## YOU SHOULD Handle

* architecture decisions
* debugging
* testing
* Firebase validation
* navigation flow
* UI polishing
* project direction

---

# 🎯 FINAL MVP GOAL

The final project should feel like:

```plaintext
A functional Airbnb-style mobile reservation app
```

NOT:

```plaintext
A production-ready Airbnb clone
```

Success is:

* stable functionality,
* convincing UI/UX,
* proper Firebase integration,
* clean demo flow,
* and maintainable architecture.

---

# ✅ SUCCESS CHECKLIST

## Core MVP

* Authentication
* Explore listings
* Listing details
* Reservations
* Trips
* Wishlist
* Hosting
* Profile management
* Bottom navigation

---

## Optional Polish

* Better animations
* Better filters
* Better card designs
* Improved search
* Map support
* Reviews & ratings

---

# 📌 FINAL DEVELOPMENT PRINCIPLE

The priority is:

```plaintext
Speed + Stability + Demo Quality
```

NOT:

```plaintext
Perfection or production scalability
```
