# cal.com Booking Flow — Architecture & Data Model Research

> Primary source: [calcom/cal.com](https://github.com/calcom/cal.com) (now also `calcom/cal.diy` for the community edition)
> Research date: 2026-07-05

---

## 1. Technology Stack

| Layer | Technology |
|-------|-----------|
| Framework | Next.js (React SSR/SSG) |
| API layer | tRPC (end-to-end type-safe RPC) |
| Database | PostgreSQL |
| ORM | Prisma (schema at `packages/prisma/schema.prisma`) |
| UI | Tailwind CSS + custom components |
| Video | Daily.co integration |
| Testing | Vitest, Playwright (E2E) |

---

## 2. Core Data Model (Entities & Relationships)

All entity definitions are in `packages/prisma/schema.prisma`.

### 2.1 User
**File:** `packages/prisma/schema.prisma` — model `User`

Key fields:
- `id`, `email`, `username`, `name`, `timeZone` (default "Europe/London")
- `weekStart` (default "Sunday") — which day the calendar week starts on
- `bufferTime` (default 0) — global buffer before/after all events
- `defaultScheduleId` — FK to the user's default Schedule
- `completedOnboarding` — boolean flag
- `hideBranding` — for white-label

Relationships:
- `eventTypes` → `EventType[]` (many-to-many via `user_eventtype`)
- `schedules` → `Schedule[]` (one-to-many)
- `availability` → `Availability[]` (one-to-many, user-level availability)
- `bookings` → `Booking[]`
- `destinationCalendar` → `DestinationCalendar?` (where events are written)
- `defaultScheduleId` → default Schedule FK

### 2.2 EventType
**File:** `packages/prisma/schema.prisma` — model `EventType`

This is the central entity. It represents a bookable meeting type (e.g., "30 Minute Consultation").

Key configuration fields:
| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `title` | String | — | Display name |
| `slug` | String | — | URL-safe identifier |
| `length` | Int | — | Duration in minutes |
| `slotInterval` | Int? | — | If set, restricts slot start times to this interval (e.g., every 15 min) |
| `offsetStart` | Int | 0 | How far from now slots start being offered (in minutes) |
| `minimumBookingNotice` | Int | 120 | How far in advance bookers must book (in minutes, default 2 hours) |
| `beforeEventBuffer` | Int | 0 | Minutes blocked before each event |
| `afterEventBuffer` | Int | 0 | Minutes blocked after each event |
| `seatsPerTimeSlot` | Int? | — | If set, enables "seats" (group event) mode |
| `scheduleId` | Int? | — | FK to the Schedule that defines availability windows |
| `timeZone` | String? | — | Override timezone (otherwise uses user's) |
| `locations` | Json? | — | Array of location types (in-person, Zoom, Google Meet, etc.) |
| `periodType` | enum | UNLIMITED | UNLIMITED / ROLLING / ROLLING_WINDOW / RANGE |
| `periodDays` | Int? | — | Days into the future for ROLLING/ROLLING_WINDOW |
| `periodStartDate` / `periodEndDate` | DateTime? | — | Date range for RANGE type |
| `requiresConfirmation` | Boolean | false | Host must approve bookings before they're confirmed |
| `requiresConfirmationWillBlockSlot` | Boolean | false | Even unconfirmed bookings block the slot |
| `bookingLimits` | Json? | — | Limit bookings per day/week/month/year |
| `durationLimits` | Json? | — | Limit total duration per day/week/month/year |
| `recurringEvent` | Json? | — | Recurring event configuration |
| `schedulingType` | enum? | — | ROUND_ROBIN / COLLECTIVE / MANAGED (team events) |
| `hidden` | Boolean | false | Hide from listing |
| `disableGuests` | Boolean | false | Disallow additional guests |
| `customInputs` | — | — | Extra fields on booking form (EventTypeCustomInput relation) |
| `maxActiveBookingsPerBooker` | Int? | — | Limit per-booker active bookings |
| `isInstantEvent` | Boolean | false | Instant booking without scheduling |
| `bookingRequiresAuthentication` | Boolean | false | Must be logged in to book |
| `requiresCancellationReason` | enum? | MANDATORY_HOST_ONLY | Cancellation reason requirements |

Relationships:
- `hosts` → `Host[]` — who is hosting (for team event types)
- `users` → `User[]` — users who can edit (via `user_eventtype`)
- `owner` → `User?` — the owner
- `schedule` → `Schedule?` — the availability schedule
- `bookings` → `Booking[]`
- `team` → `Team?` — if a team event type
- `availability` → `Availability[]` — event-type-specific availability overrides
- `selectedCalendars` → `SelectedCalendar[]` — which calendars to check for conflicts
- `destinationCalendar` → `DestinationCalendar?` — where to write event
- `customInputs` → `EventTypeCustomInput[]`
- `children` / `parent` — for managed event types
- `hashedLink` → `HashedLink[]` — secret links with expiry / usage limits

Unique constraints:
- `@@unique([userId, slug])` — user's event types have unique slugs
- `@@unique([teamId, slug])` — team event types have unique slugs

### 2.3 Schedule
**File:** `packages/prisma/schema.prisma` — model `Schedule`

Represents a named set of weekly availability windows.

Fields:
- `id`, `userId`, `name`, `timeZone`

Relationships:
- `user` → `User` (many schedules per user)
- `eventType` → `EventType[]` (many event types can use one schedule)
- `availability` → `Availability[]` (the weekly time windows)
- `Host` → `Host[]` (schedule assigned to a host on an event type)

### 2.4 Availability
**File:** `packages/prisma/schema.prisma` — model `Availability`

Represents a single time window. Can be attached to a User, an EventType, or a Schedule.

Fields:
| Field | Type | Purpose |
|-------|------|---------|
| `days` | Int[] | Array of day numbers (0=Sunday, 1=Monday, … 6=Saturday) |
| `startTime` | DateTime (Time) | Start time (hour:minute) |
| `endTime` | DateTime (Time) | End time (hour:minute) |
| `date` | DateTime? (Date) | If set, overrides for a specific date only (date-specific override) |
| `userId` | Int? | FK to User (user-level override, e.g., vacation) |
| `eventTypeId` | Int? | FK to EventType (override for a specific event type) |
| `scheduleId` | Int? | FK to Schedule (part of a named schedule) |

Critical design: the `date` field makes a row either a **recurring weekly rule** (if `date` is null) or a **date-specific override** (if `date` is set). A date override takes precedence over the weekly rules for that specific date.

### 2.5 Booking
**File:** `packages/prisma/schema.prisma` — model `Booking`

Represents a confirmed booking (or pending confirmation).

Key fields:
| Field | Type | Purpose |
|-------|------|---------|
| `uid` | String (unique) | Public booking ID |
| `startTime` | DateTime | Slot start |
| `endTime` | DateTime | Slot end |
| `title` | String | Event title |
| `status` | BookingStatus | ACCEPTED / PENDING / CANCELLED / REJECTED / AWAITING_HOST |
| `eventTypeId` | Int? | FK to EventType |
| `userId` | Int? | FK to the host user |
| `userPrimaryEmail` | String? | Host's email at time of booking |
| `location` | String? | Meeting location |
| `responses` | Json? | Booker's form responses |
| `customInputs` | Json? | Custom input responses |
| `metadata` | Json? | App-specific metadata |
| `fromReschedule` | String? | UID of original booking if rescheduled |
| `recurringEventId` | String? | Groups recurring bookings |

Relationships:
- `attendees` → `Attendee[]` — who's attending
- `references` → `BookingReference[]` — external calendar event refs (Google, Outlook, etc.)
- `eventType` → `EventType?`
- `user` → `User?` — the host
- `payment` → `Payment[]`

### 2.6 Attendee
**File:** `packages/prisma/schema.prisma` — model `Attendee`

Fields: `id`, `email`, `name`, `timeZone`, `locale` (default "en"), `bookingId`, `noShow` (Boolean)

The guest who booked.

### 2.7 BookingReference
**File:** `packages/prisma/schema.prisma` — model `BookingReference`

External calendar event references. One booking can have multiple references (one per calendar integration).

Fields: `type` (e.g., "google_calendar"), `uid`, `meetingId`, `meetingPassword`, `meetingUrl`, `bookingId`, `credentialId`

### 2.8 Host
**File:** `packages/prisma/schema.prisma` — model `Host`

Join table between User and EventType for team events.

Fields: `userId`, `eventTypeId` (composite PK), `isFixed` (always included), `priority`, `weight` (for round-robin), `scheduleId` (per-host schedule override)

### 2.9 SelectedCalendar
**File:** `packages/prisma/schema.prisma` — model `SelectedCalendar`

Which external calendars to check for busy/conflict detection.

Fields: `userId`, `integration` (e.g., "google_calendar"), `externalId`, `credentialId`, `eventTypeId` (optional, per-event-type override)

### 2.10 Other supporting entities

- **Team** — model with `id`, `name`, `slug`, parent-child org hierarchy
- **DestinationCalendar** — where to write events (per user or per event type)
- **Credential** — OAuth tokens for calendar integrations
- **Payment** — payment records
- **Webhook** — webhook subscriptions
- **HashedLink** — secret booking links with expiry/max-usage
- **EventTypeCustomInput** — extra fields on the booking form (text, number, bool, radio, phone)
- **Profile** — organization-scoped user profile (replaces simple User for multi-org setups)
- **HostGroup** — groups of hosts for team event types
- **HostLocation** — per-host location overrides

---

## 3. How Event Types Are Created and Configured

### 3.1 Event Type Creation (Form/API)

Source files:
- `packages/features/eventtypes/components/` — React components for the event type creation/editing form
- `packages/features/eventtypes/lib/` — business logic
- `packages/features/eventtypes/service/` — service layer
- `packages/features/eventtypes/repositories/` — data access layer

The event type creation form collects:
1. **Basic info**: title, description, slug, duration (`length` in minutes)
2. **Location**: one or more locations (in-person, Zoom, Google Meet, Teams, phone, etc.)
3. **Schedule/availability**: which Schedule to use (or create a new one inline)
4. **Booking limits**: how far in the future, minimum notice, booking frequency limits
5. **Advanced**: buffers (before/after), event color, requires confirmation, seats, recurring, custom inputs/fields
6. **Calendar connection**: which calendars to check for conflicts (`SelectedCalendar`)

### 3.2 Period Type (How Far Into the Future Slots Are Shown)

The `periodType` field on EventType controls this:
- `UNLIMITED` — Show slots forever (default)
- `ROLLING` — Show slots for the next N `periodDays` calendar days
- `ROLLING_WINDOW` — Show slots within a rolling window of N business days
- `RANGE` — Show slots only between `periodStartDate` and `periodEndDate`

### 3.3 Slot Interval

The `slotInterval` field controls time granularity. If set to 15, bookers can only select times at :00, :15, :30, :45. If null, the slot interval matches the event length.

---

## 4. The Booking Flow

### 4.1 High-Level Flow

The booking flow is documented at `headless-routing-to-booking-flow.md` and implemented across several packages:

1. **Booker visits booking page** → `/[user]/[eventTypeSlug]` or via hashed link
2. **Select date** → page loads available dates (date picker)
3. **Select time slot** → page loads available times for the selected date
4. **Fill in details** → name, email, custom inputs, additional guests
5. **Confirm** → POST booking request, which runs validation + creates the booking

### 4.2 Booking Page Implementation

Source: `apps/web/` directory
- `apps/web/pages/[user]/[type].tsx` — the public booking page for a user's event type
- `apps/web/pages/team/[slug]/[type].tsx` — team booking page
- `apps/web/pages/d/[link]/[slug].tsx` — hashed/direct booking link page

The booking page follows a multi-step flow:
1. **Date/time selection** — booker picks a date, available slots are shown
2. **Form** — booker fills in name, email, additional guests, custom inputs
3. **Confirmation** — final review before submitting

### 4.3 Slot Generation (Availability Computation)

Source: `packages/lib/availability.ts` and `packages/lib/builders/` directory

The slot generation algorithm:

1. **Get the Schedule's Availability entries** — weekly time windows (`days[]`, `startTime`, `endTime`)
2. **Apply date overrides** — `Availability` rows with a `date` field override weekly rules for specific dates
3. **Apply buffer times** — from `EventType.beforeEventBuffer` and `EventType.afterEventBuffer`
4. **Apply user-level buffer** — from `User.bufferTime`
5. **Apply minimum booking notice** — from `EventType.minimumBookingNotice` (default 120 min). Don't show slots within this window from "now".
6. **Check for conflicts** — query bookings in the time window (from `SelectedCalendar` busy times and `Booking` table records)
7. **Remove busy slots** — subtract booked times from available times
8. **Split by event length + interval** — generate discrete start times based on `EventType.length` and `EventType.slotInterval`
9. **Apply period restrictions** — only slots within the configured `periodType` window

Calendar conflict checking (`packages/lib/CalendarService.ts`):
- Queries external calendar APIs (Google Calendar, Outlook, etc.) for busy/blocked times
- Fetches existing `Booking` records for the host
- Merges external and internal busy times into a unified set

### 4.4 Booking Confirmation (Backend Handler)

Source: `packages/features/bookings/lib/handleNewBooking/`

The booking handler is split into multiple files, each handling a specific concern:

| File | Purpose |
|------|---------|
| `getBookingData.ts` | Parse and validate the incoming booking request |
| `getEventType.ts` / `getEventTypesFromDB.ts` | Load the EventType with all required relations |
| `loadUsers.ts` / `loadAndValidateUsers.ts` | Load host users and validate they exist |
| `ensureAvailableUsers.ts` | For team events (round-robin/collective), determine which hosts are available |
| `checkBookingAndDurationLimits.ts` | Validate against `bookingLimits` and `durationLimits` |
| `checkActiveBookingsLimitForBooker.ts` | Validate against `maxActiveBookingsPerBooker` |
| `validateBookingTimeIsNotOutOfBounds.ts` | Ensure the requested time is still valid (not past, within period limits) |
| `validateEventLength.ts` | Ensure the event length matches what was expected |
| `getRequiresConfirmationFlags.ts` | Determine if confirmation email is required |
| `createBooking.ts` | Create the Booking + Attendee + BookingReference records in DB |
| `handleAppsStatus.ts` | Update app-specific status (payment status, etc.) |
| `getCustomInputsResponses.ts` | Process custom input responses |

**Booking validation sequence** (approximate order):
1. Load EventType from DB (with schedule, hosts, users, custom inputs, selected calendars)
2. Check if the slot is still available (it might have been taken between page load and submit)
3. Check `minimumBookingNotice` — is the booking time still far enough in the future?
4. Check `bookingLimits` — has the booker exceeded daily/weekly/monthly/yearly limits?
5. Check `durationLimits` — has the total booked duration exceeded limits?
6. Check `maxActiveBookingsPerBooker` — does the booker have too many active bookings?
7. For team events: run round-robin selection or collective availability check
8. Check calendar conflicts via external calendar APIs (Google/Outlook)
9. If `requiresConfirmation`: set status to `PENDING`, send approval email to host
10. If not requires confirmation: set status to `ACCEPTED`, send confirmation emails
11. Create booking references in external calendars (Google Calendar event, etc.)
12. Fire webhooks for BOOKING_CREATED / BOOKING_REQUESTED events

### 4.5 Booking Status Lifecycle

```
                   ┌─────────────────────────────┐
                   │   Booker submits booking     │
                   └─────────────┬───────────────┘
                                 │
                   ┌─────────────▼───────────────┐
                   │ requiresConfirmation?        │
                   └──┬──────────────────────┬───┘
                      │ Yes                  │ No
              ┌───────▼───────┐      ┌───────▼───────┐
              │   PENDING     │      │   ACCEPTED    │
              └───────┬───────┘      └───────────────┘
                      │
         ┌────────────┼────────────┐
         ▼            ▼            │
    ┌─────────┐ ┌──────────┐      │
    │ACCEPTED │ │ REJECTED │      │
    └─────────┘ └──────────┘      │
                                  │
                          ┌───────▼───────┐
                          │  CANCELLED    │ (at any point)
                          └───────────────┘
```

---

## 5. Key Business Rules

### 5.1 Buffers

- **beforeEventBuffer** (EventType level, minutes): Blocks time before each event. E.g., value of 15 means no slots start within 15 minutes of an existing booking's start. Default: 0.
- **afterEventBuffer** (EventType level, minutes): Blocks time after each event. E.g., value of 15 means no slots start within 15 minutes after an existing booking's end. Default: 0.
- **bufferTime** (User level, minutes): Applies to all event types for that user. Added to the event-type-level buffers. Default: 0.

### 5.2 Minimum Booking Notice

`EventType.minimumBookingNotice` (default 120 min = 2 hours): Bookers cannot book a slot that starts sooner than this many minutes from "now." Combined with `offsetStart`, which is how far from "now" the slot picker starts showing slots.

### 5.3 Booking Limits

`EventType.bookingLimits` (JSON): Count-based limits per time window:
```json
{
  "PER_DAY": 5,
  "PER_WEEK": 20,
  "PER_MONTH": 50,
  "PER_YEAR": 200
}
```

`EventType.durationLimits` (JSON): Duration-based limits per time window (in minutes):
```json
{
  "PER_DAY": 240,
  "PER_WEEK": 1200
}
```

Team-level booking limits also exist on the `Team.bookingLimits` field.

Validation happens in `checkBookingAndDurationLimits.ts`.

### 5.4 Per-Booker Limit

`EventType.maxActiveBookingsPerBooker`: Limits how many active (non-cancelled) bookings a single email can have for this event type. Checked in `checkActiveBookingsLimitForBooker.ts`.

### 5.5 Seats (Group Events)

When `EventType.seatsPerTimeSlot` is set, multiple attendees can book the same time slot (up to the seat limit). Additional fields:
- `seatsShowAttendees` — show who else is attending
- `seatsShowAvailabilityCount` — show remaining seat count

A `BookingSeat` join model manages individual seat assignments.

### 5.6 Requires Confirmation

When `requiresConfirmation` is true:
- Booking is created with `PENDING` status
- Host receives an email to approve/reject
- If `requiresConfirmationWillBlockSlot` is also true, the slot is blocked even before confirmation

### 5.7 Recurring Events

Configured via `EventType.recurringEvent` (JSON). The booking flow allows the booker to select multiple dates in a recurring pattern. Creates multiple `Booking` records linked by `recurringEventId`.

### 5.8 Round-Robin & Collective Scheduling (Teams)

Controlled by `EventType.schedulingType`:
- **ROUND_ROBIN**: Each booking is assigned to the next available host in rotation. Uses `Host.priority` and `Host.weight` for ordering.
- **COLLECTIVE**: All hosts must be available simultaneously; booking is created only if every host is free.
- **MANAGED**: Parent event type delegates to child event types per managed user.

---

## 6. URL Routing Structure

Based on the Next.js pages directory (`apps/web/pages/`):

| URL Pattern | Purpose |
|-------------|---------|
| `/[user]/[type]` | Public booking page for a user's event type |
| `/team/[slug]/[type]` | Team event type booking page |
| `/d/[link]/[slug]` | Hashed/direct link booking page (with optional expiry and max usage) |
| `/booking/[uid]` | Booking confirmation/status page |
| `/reschedule/[uid]` | Reschedule an existing booking |
| `/event-types` | Event type management dashboard |
| `/availability` | Schedule management |

---

## 7. Key Architectural Patterns

### 7.1 Schedule → Availability Relationship

A `Schedule` is a named collection of `Availability` rows. An `EventType` points to ONE `Schedule`. The `Schedule` defines the weekly windows, but individual date overrides can exist as `Availability` rows with the `date` field set. The slot generation code merges:
1. The schedule's weekly availability entries (days + start-end times)
2. Any date-specific overrides (which replace or modify a day)
3. User-level availability overrides (individual vacation days, etc.)
4. Event-type-specific availability overrides

### 7.2 Calendar Conflict Checking

`SelectedCalendar` connects a user to an external calendar (via Credential). These calendars are queried for busy times during slot generation. The `CalendarService` (`packages/lib/CalendarService.ts`) handles:
- Fetching busy times from external providers (Google Calendar API, Outlook API, etc.)
- Watching for calendar changes (webhooks/channels for real-time sync)
- Caching busy times locally

### 7.3 Slot Generation Flow Summary

```
User-facing booking page
    ↓
tRPC query: getSchedule( eventTypeSlug, dateRange )
    ↓
1. Load EventType (with schedule, hosts, selectedCalendars)
2. Load Schedule.availability[] → weekly windows
3. Apply date overrides (Availability rows with date set)
4. Load existing Bookings for the time range → busy slots
5. Load external calendar busy times (via CalendarService) → busy slots
6. Merge busy times from Bookings + external calendars
7. Compute available windows = schedule windows minus busy times
8. Subtract beforeEventBuffer + afterEventBuffer + bufferTime
9. Apply minimumBookingNotice (skip slots too close to now)
10. Apply periodType window (limit how far forward to show)
11. Split windows into discrete slots (by eventType.length, respecting slotInterval)
12. Return { date: string, slots: { time: string }[] } to the UI
```

### 7.4 API Layer

All backend communication uses tRPC (not REST). The tRPC router definitions live in `packages/trpc/server/routers/`. Key routers:
- `viewer.eventTypes` — CRUD for event types
- `viewer.availability` — CRUD for availability/schedule
- `viewer.bookings` — bookings management
- `viewer.public` — public-facing endpoints (get schedule, create booking)

---

## 8. Summary: What Matters for Building a Similar System

1. **Core entities**: User, EventType, Schedule, Availability, Booking, Attendee — all defined in the Prisma schema
2. **Event type drives everything**: duration, buffers, limits, confirmation rules, seats are all on EventType
3. **Schedule is separate from EventType**: one schedule can serve many event types; schedule = named collection of weekly availability windows
4. **Availability has a dual purpose**: weekly recurring rules (days[]) AND date-specific overrides (date field)
5. **Slot generation = schedule windows minus busy times minus buffers**: the core computation
6. **Booking confirmation is multi-step**: validation of limits, busy checks, external calendar operations, webhooks
7. **External calendars are queried for conflicts**: `SelectedCalendar` + `CalendarService` pattern
8. **Team features via Host join table**: round-robin, collective, per-host schedules
9. **Confirmation flow**: `requiresConfirmation` flag toggles between immediate acceptance and host-approval flow
