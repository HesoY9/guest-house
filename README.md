# guest-house-management-system

Guest — id, name, contact info, ID/passport number
Room — id, room number, type (single/double/suite), price per night, status
Booking — id, guest, room, check-in date, check-out date, status
Invoice/Payment — links to a booking, amount, payment status
User (abstract or base) → Admin, Receptionist — for login/roles
GuestHouse or RoomService — orchestrates operations (could be a service class)
