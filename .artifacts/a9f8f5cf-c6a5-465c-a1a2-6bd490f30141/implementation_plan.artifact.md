# Implementation Plan - ExpenseTrack Project

This plan outlines the steps to complete the ExpenseTrack application, ensuring all requirements for the college MAD practical are met, including local Room database storage, camera receipt capture, and dynamic data calculation with no pre-filled samples.

## User Review Required

> [!IMPORTANT]
> The application will strictly use `₹0.00` as the initial state. All calculations will be performed dynamically from the Room database.

> [!NOTE]
> I will use the `androidx.room` dependencies compatible with the current project setup (Gradle 8.5+ and Kotlin).

## Proposed Changes

### Configuration & Dependencies

#### [MODIFY] [libs.versions.toml](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/gradle/libs.versions.toml)
- Add Room library versions and definitions.

#### [MODIFY] [build.gradle.kts](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/build.gradle.kts)
- Add Room dependencies (`room-runtime`, `room-ktx`, `room-compiler`).
- Apply the `ksp` or `kapt` plugin for Room annotation processing.

---

### Database Layer

#### [NEW] [Expense.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/database/Expense.kt)
- Relocate and ensure proper entity structure for `Expense`.

#### [NEW] [ExpenseDao.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/database/ExpenseDao.kt)
- Relocate and implement CRUD operations and summary queries.

#### [NEW] [AppDatabase.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/database/AppDatabase.kt)
- Relocate and ensure singleton pattern.

#### [DELETE] Existing database files in the root package.

---

### UI Components & Adapters

#### [NEW] [ExpenseAdapter.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/adapter/ExpenseAdapter.kt)
- Relocate and finalize the adapter for RecyclerView.

#### [DELETE] `ExpenseAdapter.kt` from the root package.

---

### Activities

#### [MODIFY] [MainActivity.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/MainActivity.kt)
- Finalize dashboard calculations and navigation to new screens.

#### [NEW] [ExpenseListActivity.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/ExpenseListActivity.kt)
- Implement full expense list with RecyclerView and empty state handling.

#### [NEW] [ExpenseDetailActivity.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/ExpenseDetailActivity.kt)
- Implement detailed view, deletion (with confirmation), and sharing functionality.

#### [NEW] [SummaryActivity.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/SummaryActivity.kt)
- Implement monthly totals, category-wise breakdown, and sharing of monthly summary.

#### [MODIFY] [AddExpenseActivity.kt](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/java/com/example/expensetrack/AddExpenseActivity.kt)
- Ensure validation rules and camera integration work as per requirements.

---

### Manifest & Resources

#### [MODIFY] [AndroidManifest.xml](file:///D:/Users/Om Patel/Projects/AndroidStudioProjects/ExpenseTrack/app/src/main/AndroidManifest.xml)
- Register all new Activities.
- Add necessary permissions for Camera (if required by the specific API implementation).

---

## Verification Plan

### Automated Tests
- I will perform a clean build to ensure all Room annotations are processed correctly.
- I will use `gradle build` to verify compilation.

### Manual Verification (Instructions for User)
1. **Initial State:** Launch the app; verify all totals show `₹0.00` and "No expenses yet".
2. **Add Expense:** Add a new expense with a specific amount and verify it appears in the list and dashboard.
3. **Edit Expense:** Change the amount of an existing expense and verify all totals update correctly.
4. **Delete Expense:** Delete an expense and verify it is removed from the list and database.
5. **Camera:** Capture a receipt and verify it is saved and displayed in details.
6. **Share:** Test "Share Expense" and "Share Summary" to see the dynamic message generation.
