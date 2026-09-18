# guest-house-management-system

Guest — id, name, contact info, ID/passport number\n
Room — id, room number, type (single/double/suite), price per night, status\n
Booking — id, guest, room, check-in date, check-out date, status\n
Invoice/Payment — links to a booking, amount, payment status\n
User (abstract or base) → Admin, Receptionist — for login/roles\n
GuestHouse or RoomService — orchestrates operations (could be a service class)\n
