# 0003 — Time rules on Meetings

## Context

Not every Meeting should be available during all of the organizer's working hours. An organizer
might want "Quick Call" available only on Monday afternoons and Wednesday evenings.

## Decision

Add `MeetingTimeRule` — a child entity of Meeting with `weekday`, `startTime`, `endTime`.
Multiple rules can be attached to one Meeting (e.g., Mon 14-17 and Wed 17-19).

During slot generation, the effective time window for a given day is the **intersection** of:
1. The Availability window for that day (from AvailabilityRule or DateOverride)
2. The MeetingTimeRule for that weekday (if any)

If no MeetingTimeRule exists for a Meeting, the Meeting is available during the full
Availability window (backward-compatible).

## Consequences

- An empty `timeRules` array means "available whenever the organizer is" — backward-compatible
  with meetings created before this feature
- Frontend: meeting editor includes a weekday + time picker for adding rules
- Workspace cards and booking cards display the time rules summary
  (e.g., "Mon 14:00-17:00 · Wed 17:00-19:00")
