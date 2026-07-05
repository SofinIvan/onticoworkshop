# 0001 — Separate Meeting from Availability

## Context

The initial design merged meeting configuration (duration, buffers, slot interval) with the
organizer's availability rules into a single `OnlineCallSettings` entity tied 1:1 to the User.

## Decision

Split into two independent entities:

- **Meeting** — a bookable meeting template: title, duration, slot interval, buffers, notice period,
  time restrictions (MeetingTimeRule). An organizer can have multiple Meetings.
- **Availability** — the organizer's availability rules: weekly recurring windows (AvailabilityRule)
  and date-specific overrides (DateOverride). Independent of any specific Meeting.

Slot generation intersects the two: `Availability` provides the time windows,
`MeetingTimeRule` (optionally) narrows them per-meeting.

## Consequences

- An organizer can offer different meeting types with different availability windows
  (e.g., "Consultation" on weekdays, "Strategy Session" on Tuesdays only)
- `OnlineCallSettings` entity removed; its fields absorbed into `Meeting`
- Slot generation logic now intersects two data sources instead of one
- UUID-based public links are per-Meeting, enabling direct sharing of specific meeting types
