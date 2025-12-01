# PuraTrip: The Ultimate Collaborative Travel Companion

[**Get it on Google Play**](https://play.google.com/store/apps/details?id=com.ysdigi.puratrip)

PuraTrip is a modern, mobile-first application designed to eliminate the chaos of group travel. It serves as a centralized hub for friends and family to seamlessly organize every aspect of their journey, from collaborative planning and photo sharing to transparent expense tracking and settlement. Built with a focus on user experience and security, PuraTrip ensures that every traveler is on the same page, allowing the group to focus on making memories instead of managing logistics.

---

## ✨ Features

### First-Time User Experience: A Guided Welcome

The first time a user opens PuraTrip, they are greeted with a beautiful and intuitive **Onboarding Guide**.

*   **Feature Showcase:** This multi-page, swipeable guide introduces the app's core features: creating trips, sharing photos, tracking expenses, and collaborative planning. Each page features a clear icon, a concise title, and a simple description.
*   **Intuitive Navigation:** To ensure users understand the swiping gesture, a subtle, animated **"swipe left" icon** is displayed on all but the last page, providing a clear visual cue.
*   **One-Time Experience:** The app uses `SharedPreferences` to track whether the user has completed the guide. It only ever appears on the very first launch, ensuring a quick entry into the app on all subsequent uses.

### Secure and Flexible Authentication

Getting into the app is simple and secure, offering multiple pathways for user convenience.

*   **Email & Password Registration:** Users can sign up with their name, email, and a password. The system includes input validation to ensure a valid email format and a secure password length.
*   **Google Sign-In:** For quick and easy access, users can sign in with their Google account. The "Sign in with Google" button is designed to match official branding guidelines for a familiar and trustworthy experience. A **loading indicator** is displayed after the button is tapped to provide feedback while the authentication process completes.
*   **Soft Email Verification:** New users are not required to verify their email before using the app. A verification email is automatically sent, but they can proceed directly to the home screen. A detailed confirmation message is shown, informing them that the email has been sent and advising them to check their spam folder.

### The Home Screen: Your Travel Dashboard

After logging in, the user lands on the Home Screen, which acts as their central dashboard for all their adventures.

*   **Personalized Welcome:** The top bar greets the user by their display name (e.g., "Welcome, Yash").
*   **Trip List:** The main content area displays a list of all trips the user is a part of. Each `TripItem` provides an at-a-glance summary, including:
    *   Trip Name
    *   Number of users
    *   Total number of photos
    *   Total number of expenses
    *   Total trip cost, displayed in the user's preferred currency.
*   **Create a New Trip:** A Floating Action Button (+) allows users to create a new trip. This opens a dialog where they can set a trip name and invite friends by adding their email addresses. The creator is automatically added to the trip.
*   **Profile Navigation:** An icon in the top bar allows users to navigate to their personal Profile Screen.

### The Profile Screen: Manage Your Identity

The Profile Screen gives users control over their personal information and preferences.

*   **Edit Display Name:** Users can update their name, which is then reflected across the app.
*   **Currency Preference:** Users can select their preferred currency from a list (e.g., INR, USD, EUR). This preference is saved and used to display monetary values on the home screen. The default currency for new users is **INR**.
*   **Verification Status:** A green checkmark is displayed next to the user's name if their email is verified. If not, a button appears allowing them to **resend the verification email**.
*   **Password Management:** A "Change Password" button sends a secure, Firebase-generated password reset link to the user's email.
*   **Logout:** A dedicated button to sign out of the app.

### Trip Details: The Collaborative Core

Tapping on a trip from the home screen opens the detailed view, organized into three main tabs: Photos, Plan, and Payments.

#### 1. The Photos Tab

This is the shared gallery for the trip.

*   **Photo Upload:** Users can add photos from their device's gallery or take a new photo with the camera.
*   **Full-Screen Viewer:** Tapping on any photo opens it in a full-screen, immersive dialog.
    *   **Pinch-to-Zoom:** Users can use standard pinch and drag gestures to zoom in and pan around the photo.
    *   **Double-Tap to Reset:** A double-tap on a zoomed photo instantly resets it to its original size and position.
*   **Conditional Deletion:**
    *   A **delete button** is visible in the full-screen viewer **only if** the current user is the one who uploaded the photo.
*   **Selection Mode:**
    *   To select multiple photos for bulk actions, a user must now **long-press** on a photo.
    *   Once in selection mode, single taps will add or remove photos from the selection.
*   **Bulk Actions:** A special top bar appears in selection mode with options to **Download** or **Delete** all selected photos.
*   **Sorting & Grouping:** The gallery includes controls to sort photos by date, size, or uploader, and to group them by date or uploader.
*   **Empty State:** If no photos have been uploaded, a helpful message guides the user to tap the "+" button to add the first one.

#### 2. The Plan Tab

This is a shared, rich-text editor for collaborative trip planning.

*   **Rich Text Editing:** Users can format text with **bold**, *italic*, and <u>underline</u> styles. They can also highlight text, change font sizes, and insert hyperlinks.
*   **View vs. Edit Mode:** The plan is displayed in a read-only format by default. Tapping the "Edit" FAB switches to an interactive editor. Tapping "Save" commits the changes for all users to see.
*   **Empty State:** If the plan is empty, a message prompts the user to tap the "Edit" button to start planning.

#### 3. The Payments Tab

This is a powerful tool for transparently managing group expenses.

*   **Full-Screen Scrolling:** The entire screen—including the balance summary, debts, and expense list—scrolls as a single unit for a smooth experience.
*   **Balance Summary:** A card at the top shows each user's current balance.
    *   **Color-Coding:** Balances are color-coded: **green** for users who are owed money and **red** for users who owe money.
*   **Debts to Settle:** This section simplifies the balances into a clear list of who needs to pay whom to clear all debts.
*   **"Settle Up" Feature:** A "Settle Up" button opens a dialog where users can record a direct payment from one user to another. This creates a special "settlement" transaction.
*   **Expense List:**
    *   **Sorting & Grouping:** Expenses are sorted with the newest on top and are grouped by month (e.g., "November 2025").
    *   **Visual Cues:** Each expense item has an icon: a **receipt** for regular expenses and a **settlement icon** for "Settle Up" payments.
    *   **Color-Coding:** Expense items are highlighted with a background color: **green** if the current user paid for it, and **red** if they are part of the split.
    *   **Split Count:** Each item displays how many people the expense was split with.
*   **Expense Details & Editing:**
    *   Tapping an expense opens a details dialog showing all information, including who created the expense.
    *   An **"Edit" button** appears in this dialog **only if** the current user is the one who created the expense. Clicking it opens a pre-filled `EditExpenseDialog`.
*   **Add/Edit Expense Dialogs:**
    *   **Validation:** The dialogs include validation to ensure the amount is a number greater than zero and that the description is not empty.
    *   **Intuitive UI:** The "Paid by" field is a clear, dropdown-style selector. The "Split with" section uses checkboxes for easy selection of multiple users.

---

### 🛠️ Under-the-Hood

*   **Full Dark Mode Support:** The app has a complete, professionally designed dark theme that ensures excellent readability and visual comfort in low-light environments. All colors are theme-aware and adapt automatically.
*   **Robust Security:** Firebase App Check is integrated to protect the app's backend from abuse, ensuring that requests are coming from legitimate app instances.
*   **Modern Architecture:** The app leverages modern Android development practices, including Jetpack Compose for the UI, Kotlin Coroutines for asynchronous operations, and a ViewModel/Repository pattern for a clean, scalable architecture.
*   **Automated Versioning:** The build process is configured to automatically increment the app's version code and name, ensuring consistency and simplifying the release process.
