# guest-house-management-system

## Table of Contents
* [Go to GitHub Basics](#github-basics)
* [Go to Basic Entity Types](#basic-entity-types)
* [Go to Requirements](#requirements)

## GitHub basics
### step 01
* Go to the folder you wish to work in your terminal:
```bash
git clone https://github.com/HesoY9/guest-house.git
cd guest-house 
```

### step 02
* Creates a new branch named 'add-payment-logic' and switches you onto it (replace add-payment-logic with your branch name).
```bash
git checkout -b add-payment-logic
```
* Do all your coding inside IntelliJ now.

### step 03
* Check what files were changed:
```bash
git status
```
* Stage ALL modified files (get them ready to save):
```bash
git add .
```
* Commit the files with a clear, descriptive message:
```bash
git commit -m "Add Payment entity and PaymentMethod enum"
```
* Uploads your isolated branch to GitHub (replace add-payment-logic with your branch name):
```bash
git push origin add-payment-logic
```
* You will be asked to authenticate using your browser or a Personal Access Token.

### Step 04
* Go to GitHub.com in your web browser.
* Click the yellow **"Compare & pull request"** button to submit your branch for review.
* Wait for the repository owner to click **Merge**.

### Step 05
* Once merged, switch your local terminal back to the main branch:
```bash
git checkout main
```
* Pull down the newly merged code from GitHub so your machine is up-to-date:
```bash
git pull origin main
```
* Safely delete the temporary local branch since its changes are now safe in main:
```bash
git branch -d add-payment-logic
```

## Basic Entity Types


#### Entity types for major classes:
```text
User        =   id, username, passwordHash, role (enum: ADMIN, RECEPTIONIST, HOUSEKEEPING, MANAGER)   
Guest       =   id, idOrPassport, name, contact, numberOfGuests 
Room        =   id, roomNumber, type, price, status (enum: AVAILABLE, RESERVED, OCCUPIED, CLEANING_REQUIRED)   
Reservation =   id, guest (FK), room (FK), checkInDate, checkOutDate, status (ACTIVE/CANCELLED/COMPLETED)   
Invoice     =   id, reservation (FK), totalAmount, generatedDate
Payment     =   id, invoice (FK), amount, method (enum: CASH, CARD), paymentDate
```

<FollowUp>
lol i'm learning markdown now!
</FollowUp>

## Requirements
### User Requirements (UR)

- UR1
  The system shall allow staff to manage room reservations.
- UR2
  The system shall allow staff to register guest information.
- UR3
  The system shall allow guests to check in and check out efficiently.
- UR4
  The system shall generate bills and payment receipts.
- UR5
  The system shall provide room availability information.
- UR6
  The system shall allow managers to view reports.
- UR7
  The system shall allow housekeeping staff to update room status.

### Functional Requirements (FR)

- FR1
  The system shall allow administrators to add new rooms.
- FR2
  The system shall allow administrators to edit room details.
- FR3
  The system shall allow administrators to remove rooms.
- FR4
  The system shall maintain room status as:
    - Available
    - Reserved
    - Occupied
    - Cleaning Required

### Reservation Management
- FR5
  The system shall allow staff to create reservations.
- FR6
  The system shall assign a unique reservation ID to each booking.

- FR8
  The system shall prevent double-booking of rooms.
- FR9
  The system shall allow reservation cancellation.
- FR10
  The system shall allow reservation modification.

### Guest Management
- FR11
  The system shall store guest information.
    - Guest id/passport
    - Guest name
    - Contact number
    - Check-in date
    - Check-out date

- FR12
  The system shall maintain guest history.
- FR13
  The system shall allow staff to search guests.

### Check-In / Check-Out
- FR14
  The system shall allow receptionists to perform guest check-in.
- FR15
  The system shall update room status to Occupied after check-in.
- FR16
  The system shall allow receptionists to perform guest check-out.
- FR17
  The system shall automatically calculate total charges.
- FR18
  The system shall update room status to Cleaning Required after check-out.

### Payment Management
- FR19
  The system shall generate invoices.
- FR20
  The system shall record payments.
- FR21
  The system shall support cash payments.
- FR22
  The system shall support card payments.
- FR23
  The system shall generate payment receipts.

### Housekeeping
- FR24
  The system shall allow housekeeping staff/staff to update cleaning status.
- FR25
  The system shall mark cleaned rooms as Available.

### User Management
- FR30
  The system shall support user login.
- FR31
  The system shall support user logout.
- FR32
  The system shall support role-based access control.
- FR33
  The system shall allow administrators to create user accounts.
- FR34
  The system shall allow administrators to disable user accounts.

## Non-Functional Requirements (NFR)
### Performance
- NFR1
  The system should display search results within 2 seconds.
- NFR2
  The system should support multiple simultaneous users.

### Security
- NFR3
  Passwords shall be encrypted.
- NFR4
  Only authorized users shall access sensitive information.
- NFR5
  The system shall preserve data during unexpected failures.
- NFR6
  The system shall perform automatic backups.
- NFR7
  The user interface should be easy to learn and use.
- NFR8
  Staff should be able to complete a booking within 3 minutes.

### Maintainability
- NFR9
  The system should allow future feature additions with minimal modification.

### Availability
- NFR10
  The system should be available during guest house operating hours.

### Domain Requirements
- DR1
  A room cannot be assigned to more than one active reservation during the same period.
- DR2
  Check-out date must be later than check-in date.
- DR3
  Guests must provide identification before check-in.
- DR4
  A payment receipt must be generated for every completed payment.
- DR5
  A room must be cleaned before being reassigned to another guest.

### Addressed by design no need to impement all

- NFR1
  The system should display search results within 2 seconds.
- NFR2
  The system should support multiple simultaneous users.
- NFR5
  The system shall preserve data during unexpected failures.
- NFR6
  The system shall perform automatic backups.
- NFR8
  Staff should be able to complete a booking within 3 minutes.

### Using hardcoded users
- FR33
  The system shall allow administrators to create user accounts.
- FR34
  The system shall allow administrators to disable user accounts.

### Reporting
occupancy + income -> extend other 2
- FR26
  The system shall generate occupancy reports.
- FR27
  The system shall generate income reports.
- FR28
  The system shall generate reservation reports.
- FR29
  The system shall generate daily transaction reports.