# IMPLEMENTATION PLAN 3 — PLATFORM EXPERIENCE & BOOKING SYSTEMS

## Overview

Implementation Plan 3 focuses on transforming the Airbnb MVP from a functional reservation platform into a behaviorally complete booking ecosystem.

The primary focus of this phase is:

* reservation lifecycle maturity
* availability management
* traveler trip experience
* social proof/reviews
* personalized traveler organization

This phase introduces interconnected systems that simulate real-world Airbnb behavior while preserving the current architecture:

* MVVM
* Repository Pattern
* Navigation Component
* ViewBinding
* Firebase Firestore
* Kotlin Coroutines + StateFlow

---

# Sprint Status

| Sprint | Feature                                   | Priority | Status    |
| ------ | ----------------------------------------- | -------- | --------- |
| 1      | Traveler Trips System                     | P0       | ⏳ Pending |
| 2      | Calendar Blocking & Reservation Conflicts | P0       | ⏳ Pending |
| 3      | Wishlist Categories / Collections         | P1       | ⏳ Pending |
| 4      | Reviews & Ratings System                  | P1       | ⏳ Pending |

---

# Sprint 1 — Traveler Trips System

## Priority

P0 — Critical Booking Experience

## Goal

Transform the Trips screen into a full traveler reservation tracking experience similar to Airbnb.

The current implementation only lists reservations.
This sprint upgrades Trips into a structured booking management system.

---

## Core Features

### Active Reservation Tracking

Trips should display:

* listing title
* listing image
* reservation status
* host name
* host avatar
* total paid
* reservation code
* check-in/check-out
* guest count
* trip countdown
* booking status

---

### Reservation States

Supported states:

* Pending
* Confirmed
* Upcoming
* Active Stay
* Completed
* Cancelled
* Rejected

---

### Trip Sections

Trips screen should be grouped into:

* Upcoming Trips
* Currently Staying
* Past Trips
* Cancelled Trips

---

### Reservation Detail Screen

Add dedicated Trip Details screen.

Features:

* booking summary
* listing snapshot
* host contact section
* reservation breakdown
* cancellation policy
* reservation timeline
* trip status chip
* review CTA after completion

---

### Smart State Transitions

Automatic lifecycle transitions:

* Confirmed → Upcoming
* Upcoming → Active Stay (check-in date reached)
* Active Stay → Completed (checkout passed)

Should occur:

* on app launch
* when trips screen loads
* optionally via repository sync utility

---

## Planned Architecture

### New Models

* `TripDetails.kt`
* `ReservationTimeline.kt`

### New Fragments

* `TripDetailsFragment.kt`

### Repository Updates

* `ReservationRepository.kt`
* `TripsRepository.kt` (optional abstraction)

### UI Updates

* `TripsFragment.kt`
* `TripAdapter.kt`
* `item_trip.xml`

---

## Firebase Considerations

### Reservation Schema Additions

Possible new fields:

```json
{
  "status": "confirmed",
  "checkedIn": false,
  "checkedOut": false,
  "reviewSubmitted": false
}
```

---

# Sprint 2 — Calendar Blocking & Reservation Conflict System

## Priority

P0 — Core Airbnb Booking Logic

## Goal

Implement real availability management for hosts and conflict prevention for travelers.

This sprint upgrades reservations from “basic date storage” into an actual availability engine.

---

## Core Features

### Host Calendar Blocking

Hosts can:

* manually block dates
* unblock dates
* block custom ranges
* block unavailable days

---

### Reservation Conflict Detection

Prevent:

* overlapping reservations
* reservations on blocked dates
* duplicate active bookings

Must validate:

* before reservation creation
* during date selection
* at repository write level

---

### Availability Calendar UI

Inside:

* Listing Details
* Reservation Creation
* Host Calendar

Unavailable dates should:

* appear disabled
* visually indicate blocked/reserved state

---

### Calendar Sources

Dates become unavailable from:

1. confirmed reservations
2. active stays
3. manual host blocks

---

## Conflict Logic

### Invalid Cases

* overlaps existing confirmed reservation
* overlaps blocked host dates
* checkout <= check-in
* reservation in past

---

## Planned Architecture

### New Models

* `BlockedDate.kt`
* `AvailabilityRange.kt`

### New Utilities

* `ReservationConflictValidator.kt`
* `CalendarAvailabilityMapper.kt`

### New Repositories

* `CalendarRepository.kt`

### New Fragments

* `HostCalendarFragment.kt` expansion

---

## Firestore Structure

### New Collection

```plaintext
blocked_dates/
```

Example:

```json
{
  "listingId": "abc123",
  "startDate": timestamp,
  "endDate": timestamp,
  "reason": "Host unavailable"
}
```

---

## Important Rules

### Reservation Write Protection

Conflict validation MUST happen:

1. UI Layer
2. ViewModel Layer
3. Repository Layer

Repository validation is mandatory.

---

# Sprint 3 — Wishlist Categories / Collections

## Priority

P1 — User Retention & Organization

## Goal

Allow users to organize saved listings into custom collections similar to:

* YouTube playlists
* Instagram collections
* Airbnb wishlists

---

## Core Features

### Wishlist Collections

Users can:

* create collection
* rename collection
* delete collection
* save listing into collection
* move listings between collections

---

### Default Behavior

If user has no collections:

* auto-create “Favorites”

---

### Collection UI

Display:

* collection cover image
* listing count
* collection title

---

### Save Flow

When tapping heart icon:

* show bottom sheet
* choose collection
* optionally create new collection inline

---

## Planned Architecture

### New Models

* `WishlistCollection.kt`

### Existing Model Updates

* `Wishlist.kt`

  * add `collectionId`

---

### New Fragments

* `WishlistCollectionsFragment.kt`
* `WishlistCollectionDetailsFragment.kt`
* `CreateWishlistCollectionDialog.kt`

---

### Repository Updates

* `WishlistRepository.kt`

---

## Firestore Structure

### New Collection

```plaintext
wishlist_collections/
```

Example:

```json
{
  "collectionId": "abc",
  "userId": "uid",
  "title": "Japan Trip Ideas",
  "createdAt": timestamp
}
```

---

# Sprint 4 — Reviews & Ratings System

## Priority

P1 — Marketplace Trust Layer

## Goal

Implement Airbnb-style reviews tied to completed reservations.

Reviews should only be possible after a stay is completed.

---

## Core Features

### Review Eligibility

User can review ONLY IF:

* reservation status == completed
* reservation belongs to current user
* review not yet submitted

---

### Review Submission

Review contains:

* rating (1–5)
* title
* written review
* reviewer name
* reviewer avatar
* listingId
* reservationId
* createdAt

---

### Listing Detail Reviews Section

Display:

* average rating
* review count
* review cards
* recent reviews

---

### Host Metrics

Hosts should see:

* average property rating
* total reviews
* recent review activity

---

## Planned Architecture

### New Models

* `Review.kt`
* `ReviewSummary.kt`

### New Fragments

* `CreateReviewFragment.kt`

### Repository Updates

* `ReviewRepository.kt`
* `ListingRepository.kt`

---

## Firestore Structure

### New Collection

```plaintext
reviews/
```

Example:

```json
{
  "reviewId": "rev123",
  "listingId": "listing123",
  "reservationId": "reservation123",
  "userId": "user123",
  "rating": 5,
  "comment": "Amazing stay!",
  "createdAt": timestamp
}
```

---

# Cross-System Dependencies

## Reservation System Dependencies

The following systems now depend on reservations:

* Trips
* Reviews
* Calendar Availability
* Host Dashboard

Reservation integrity becomes mission-critical.

---

# Recommended Implementation Order

## Phase Order

### 1. Calendar Blocking & Conflicts

Do FIRST because:

* reservation logic depends on it
* trips/reviews require accurate reservation states

---

### 2. Traveler Trips System

Do SECOND because:

* uses finalized reservation states
* enables review eligibility

---

### 3. Reviews System

Do THIRD because:

* depends on completed trips

---

### 4. Wishlist Collections

Do LAST because:

* isolated feature
* lower business-criticality

---

# Technical Risks

## Major Risks

### 1. Reservation Race Conditions

Two users booking simultaneously.

Mitigation:

* repository-level validation
* Firestore transaction checks
* final availability revalidation before write

---

### 2. Date Normalization

Timezone mismatches.

Mitigation:

* normalize all dates to local midnight
* centralize date utilities

---

### 3. Reservation Lifecycle Drift

Statuses becoming outdated.

Mitigation:

* lifecycle sync utility on app launch
* repository-driven reconciliation

---

# Future Enhancements (Post Plan 3)

## Potential Plan 4 Features

* Push notifications
* Real-time messaging
* Payments & refunds
* Multi-image galleries
* Search & maps
* Dynamic pricing
* Smart recommendations
* Host analytics dashboard
* Check-in instructions
* Identity verification
* Instant booking

---

# Success Criteria

Implementation Plan 3 is considered complete when:

## Booking System

* reservations cannot overlap
* blocked dates work correctly
* host availability is enforced

## Traveler Experience

* trips show full reservation lifecycle
* users can track active stays

## Trust Layer

* completed reservations can be reviewed
* listing ratings display correctly

## Personalization

* users can organize wishlists into collections

---

# Final Goal

At the completion of Implementation Plan 3, the platform should behave much closer to a real Airbnb ecosystem:

* reliable booking engine
* host availability management
* traveler trip tracking
* social trust/reviews
* personalized traveler organization

This phase transitions the app from “Airbnb-inspired UI” into a behaviorally accurate booking platform.
