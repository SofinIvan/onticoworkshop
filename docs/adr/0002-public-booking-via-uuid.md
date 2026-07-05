# 0002 — Public booking via UUID

## Context

Users need to share a link that lets guests book a meeting without navigating the full UI or
knowing the organizer's username.

## Decision

Each Meeting receives a UUID on creation. A public endpoint `GET /public/meeting?uuid=X`
returns meeting + organizer info without authentication. The frontend detects
`/?meeting={uuid}` in the URL and renders `PublicBookingPage` — a standalone booking form.

Guests can create bookings via the existing `POST /booking` endpoint, which accepts
`meetingUuid` as an alternative to the internal `meetingId`. The backend resolves the UUID
to the meeting internally.

## Consequences

- No authentication required for the public booking flow
- Each Meeting is independently shareable via its UUID link
- `PublicController` added for UUID-based endpoints
- `PublicBookingPage` component handles the guest-facing UX
- Backend resolves UUIDs for booking creation, keeping internal IDs opaque
