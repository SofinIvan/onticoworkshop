# 0004 — Duplicate booking prevention

## Context

A guest should not be able to book the same meeting more than once. Without a check,
multiple identical bookings could be created accidentally or through race conditions.

## Decision

Before creating a Booking, the backend checks for an existing booking with the same
`meetingId` and `guestEmail` where `status != 'cancelled'`. If found, a `409 Conflict`
is returned with the message "You are already registered for this meeting."

Cancelled bookings are excluded from the check — a guest who cancelled can re-book.

## Consequences

- Added `BookingRepository.findByMeetingIdAndGuestEmailAndStatusNot`
- `BookingService.createBooking` checks before insertion
- Frontend catches the 409 response and displays the message in a red paper block
- The check is scoped per-meeting: a guest can book Meeting A and Meeting B independently
