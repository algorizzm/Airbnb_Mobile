# 🏠 HOSTING NAVIGATION GUIDE

## 📍 HOW TO ACCESS HOSTING MODE

### Method 1: From Profile Screen (Primary Method)

1. **Open the app**
2. **Tap Profile tab** (bottom navigation, rightmost icon)
3. **Scroll down** to find the "Hosting" card
4. **Tap "Manage Listings"** button

```
Profile Screen
    ↓
[Hosting Card]
    ↓
[Manage Listings Button]
    ↓
Host Listings Screen
```

---

## 🎯 HOSTING SCREEN FEATURES

### Host Listings Screen
Once you're in the Host Listings screen, you can:

- **View all your listings** (RecyclerView)
- **Create new listing** (tap FAB + button)
- **Edit listing** (tap "Edit" button on listing card)
- **Delete listing** (tap "Delete" button on listing card)
- **View reservations** (tap "Bookings" button on listing card)

---

## 🔧 ADDING NAVIGATION FROM OTHER SCREENS (Optional)

If you want to add navigation to hosting from other screens, follow these steps:

### Step 1: Add Navigation Action

In `main_graph.xml`, add an action from your source fragment:

```xml
<fragment
    android:id="@+id/yourSourceFragment"
    android:name="com.airbnb.ui.your.YourFragment"
    android:label="Your Screen">
    
    <!-- Add this action -->
    <action
        android:id="@+id/action_yourSourceFragment_to_hostListingsFragment"
        app:destination="@id/hostListingsFragment" />
        
</fragment>
```

### Step 2: Navigate in Code

In your fragment, use:

```kotlin
findNavController().navigate(R.id.action_yourSourceFragment_to_hostListingsFragment)
```

---

## 🐛 TROUBLESHOOTING

### Issue: "Manage Listings" button not visible

**Solution:**
- Make sure you're logged in (not in guest mode)
- Scroll down on the Profile screen
- The Hosting card appears after the "About" card

### Issue: Navigation crashes

**Solution:**
- Check that `hostListingsFragment` is defined in `main_graph.xml`
- Verify the navigation action exists
- Check Logcat for specific error messages

### Issue: Empty host listings screen

**Solution:**
- This is normal if you haven't created any listings yet
- Tap the FAB (+ button) to create your first listing

---

## 📱 COMPLETE HOSTING FLOW

### Creating Your First Listing

1. **Profile** → Tap "Manage Listings"
2. **Host Listings** → Tap FAB (+ button)
3. **Create Listing** → Fill in form:
   - Title
   - Description
   - Location
   - Property Type
   - Price per night
   - Max Guests
   - Bedrooms
   - Bathrooms
   - Amenities
   - Image URL (optional)
4. **Tap "Save Listing"**
5. **Success!** → Returns to Host Listings screen

### Editing a Listing

1. **Host Listings** → Find your listing
2. **Tap "Edit"** button
3. **Edit form** → Modify fields
4. **Tap "Save Listing"**
5. **Success!** → Returns to Host Listings screen

### Viewing Reservations

1. **Host Listings** → Find your listing
2. **Tap "Bookings"** button
3. **View reservations** → See all bookings for that listing
4. **Cancel if needed** → Tap "Cancel" button on reservation

---

## 🎨 UI ELEMENTS

### Profile Screen - Hosting Card
```
┌─────────────────────────────────┐
│ Hosting                         │
│                                 │
│ List your space and start       │
│ earning                         │
│                                 │
│ ┌─────────────────────────────┐ │
│ │   Manage Listings           │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

### Host Listings Screen
```
┌─────────────────────────────────┐
│ ← My Listings                   │
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │ [Image] Cozy Beach House    │ │
│ │         Malibu, CA          │ │
│ │         $150 / night        │ │
│ │ [Edit] [Delete] [Bookings]  │ │
│ └─────────────────────────────┘ │
│                                 │
│                          [+]    │
└─────────────────────────────────┘
```

---

## 🔑 KEY POINTS

✅ **Unified User System**
- All users can become hosts
- No separate host account needed
- Same login credentials

✅ **Easy Access**
- One tap from Profile screen
- Clear "Manage Listings" button
- Intuitive navigation flow

✅ **Complete Management**
- Create, edit, delete listings
- View and manage reservations
- Real-time updates

---

## 📊 NAVIGATION HIERARCHY

```
Main App
├── Explore (Bottom Nav)
├── Wishlists (Bottom Nav)
├── Trips (Bottom Nav)
├── Messages (Bottom Nav)
└── Profile (Bottom Nav)
    └── Hosting Card
        └── Manage Listings Button
            └── Host Listings Screen
                ├── Create Listing Screen
                ├── Edit Listing Screen
                └── Host Reservations Screen
```

---

## 🎯 QUICK REFERENCE

| Action | Location | Button/Element |
|--------|----------|----------------|
| Access Hosting | Profile Screen | "Manage Listings" button in Hosting card |
| Create Listing | Host Listings | FAB (+ button) |
| Edit Listing | Host Listings | "Edit" button on listing card |
| Delete Listing | Host Listings | "Delete" button on listing card |
| View Reservations | Host Listings | "Bookings" button on listing card |
| Cancel Reservation | Host Reservations | "Cancel" button on reservation card |

---

## 🚀 NEXT STEPS

After accessing the hosting screen:

1. **Create your first listing** using the FAB
2. **Test the form** with sample data
3. **Verify Firebase sync** in Firestore console
4. **Check real-time updates** by opening on multiple devices
5. **Test reservation management** by making a booking as a guest

---

**Happy Hosting! 🏠✨**
