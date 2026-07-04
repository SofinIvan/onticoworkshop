import "@typespec/http";

using Http;

@service(#{ title: "Calendar Booking Service" })
namespace CalendarBookingService;

alias Identifier = string;
alias IanaTimezone = string;
alias EmailAddress = string;

enum Weekday {
  monday,
  tuesday,
  wednesday,
  thursday,
  friday,
  saturday,
  sunday,
}

enum BookingStatus {
  confirmed,
  cancelled,
  rescheduled,
}

model Ok<T> {
  @statusCode _: 200;
  @body body: T;
}

model Created<T> {
  @statusCode _: 201;
  @body body: T;
}

model NoContent {
  @statusCode _: 204;
}

@error
model ErrorResponse {
  @statusCode statusCode: 400 | 404 | 409 | 422 | 500;
  code: string;
  message: string;
  target?: string;
  details?: string[];
}

model User {
  id: Identifier;
  username: string;
  displayName: string;
  email: EmailAddress;
  timezone: IanaTimezone;
  bio?: string;
  avatarUrl?: string;
  createdAt: utcDateTime;
  updatedAt: utcDateTime;
}

model CreateUserRequest {
  username: string;
  displayName: string;
  email: EmailAddress;
  timezone: IanaTimezone;
  bio?: string;
  avatarUrl?: string;
}

model UpdateUserRequest {
  username?: string;
  displayName?: string;
  email?: EmailAddress;
  timezone?: IanaTimezone;
  bio?: string;
  avatarUrl?: string;
}

model BookingProfile {
  id: Identifier;
  username: string;
  displayName: string;
  timezone: IanaTimezone;
  bio?: string;
  avatarUrl?: string;
}

model UserList {
  items: User[];
}

model OnlineCallSettings {
  userId: Identifier;
  availabilityScheduleId: Identifier;
  title: string;
  description?: string;
  durationMinutes: int32;
  timezone: IanaTimezone;
  isActive: boolean;
  minimumNoticeMinutes: int32;
  slotIntervalMinutes: int32;
  bufferBeforeMinutes: int32;
  bufferAfterMinutes: int32;
  meetingUrl?: string;
  createdAt: utcDateTime;
  updatedAt: utcDateTime;
}

model UpdateOnlineCallSettingsRequest {
  availabilityScheduleId?: Identifier;
  title?: string;
  description?: string;
  durationMinutes?: int32;
  timezone?: IanaTimezone;
  isActive?: boolean;
  minimumNoticeMinutes?: int32;
  slotIntervalMinutes?: int32;
  bufferBeforeMinutes?: int32;
  bufferAfterMinutes?: int32;
  meetingUrl?: string;
}

model AvailabilityRule {
  weekday: Weekday;
  startTime: string;
  endTime: string;
}

model DateOverride {
  date: plainDate;
  isUnavailable: boolean;
  startTime?: string;
  endTime?: string;
}

model AvailabilitySchedule {
  id: Identifier;
  userId: Identifier;
  name: string;
  timezone: IanaTimezone;
  rules: AvailabilityRule[];
  dateOverrides: DateOverride[];
  createdAt: utcDateTime;
  updatedAt: utcDateTime;
}

model CreateAvailabilityScheduleRequest {
  userId: Identifier;
  name: string;
  timezone: IanaTimezone;
  rules: AvailabilityRule[];
  dateOverrides?: DateOverride[];
}

model UpdateAvailabilityScheduleRequest {
  name?: string;
  timezone?: IanaTimezone;
  rules?: AvailabilityRule[];
  dateOverrides?: DateOverride[];
}

model AvailabilityScheduleList {
  items: AvailabilitySchedule[];
}

model TimeSlot {
  start: utcDateTime;
  end: utcDateTime;
  timezone: IanaTimezone;
}

model Guest {
  name: string;
  email: EmailAddress;
  timezone: IanaTimezone;
}

model Booking {
  id: Identifier;
  userId: Identifier;
  status: BookingStatus;
  guest: Guest;
  start: utcDateTime;
  end: utcDateTime;
  timezone: IanaTimezone;
  meetingUrl?: string;
  notes?: string;
  cancellationReason?: string;
  cancelledAt?: utcDateTime;
  rescheduledFromBookingId?: Identifier;
  createdAt: utcDateTime;
  updatedAt: utcDateTime;
}

model CreateBookingRequest {
  username: string;
  guestName: string;
  guestEmail: EmailAddress;
  guestTimezone: IanaTimezone;
  start: utcDateTime;
  notes?: string;
}

model CancelBookingRequest {
  reason?: string;
}

model RescheduleBookingRequest {
  start: utcDateTime;
  guestTimezone?: IanaTimezone;
  notes?: string;
}

model BookingList {
  items: Booking[];
}

model TimeSlotList {
  items: TimeSlot[];
}

model BookingInfo {
  profile: BookingProfile;
  onlineCall: OnlineCallSettings;
}

@route("/user")
@tag("User")
interface UserOperations {
  /** List users. */
  @get list(): Ok<UserList> | ErrorResponse;

  /** Create a user profile. */
  @post create(@body body: CreateUserRequest): Created<User> | ErrorResponse;

  /** Read a user profile. */
  @get read(@path userId: Identifier): Ok<User> | ErrorResponse;

  /** Update a user profile. */
  @patch update(
    @path userId: Identifier,
    @body body: UpdateUserRequest
  ): Ok<User> | ErrorResponse;

  /** Read the user's online call settings. */
  @route("{userId}/online-call")
  @get readOnlineCallSettings(
    @path userId: Identifier
  ): Ok<OnlineCallSettings> | ErrorResponse;

  /** Update the user's online call settings. */
  @route("{userId}/online-call")
  @patch updateOnlineCallSettings(
    @path userId: Identifier,
    @body body: UpdateOnlineCallSettingsRequest
  ): Ok<OnlineCallSettings> | ErrorResponse;
}

@route("/availability-schedule")
@tag("Availability Schedule")
interface AvailabilityScheduleOperations {
  /** List availability schedules. */
  @get list(@query userId?: Identifier): Ok<AvailabilityScheduleList> | ErrorResponse;

  /** Create an availability schedule. */
  @post create(
    @body body: CreateAvailabilityScheduleRequest
  ): Created<AvailabilitySchedule> | ErrorResponse;

  /** Read an availability schedule. */
  @get read(@path scheduleId: Identifier): Ok<AvailabilitySchedule> | ErrorResponse;

  /** Update an availability schedule. */
  @patch update(
    @path scheduleId: Identifier,
    @body body: UpdateAvailabilityScheduleRequest
  ): Ok<AvailabilitySchedule> | ErrorResponse;

  /** Delete an availability schedule. */
  @delete delete(@path scheduleId: Identifier): NoContent | ErrorResponse;
}

@route("/booking")
@tag("Booking")
interface BookingOperations {
  /** List bookings. */
  @get list(
    @query userId?: Identifier,
    @query status?: BookingStatus
  ): Ok<BookingList> | ErrorResponse;

  /** Read the online call booking details for a username. */
  @route("new")
  @get readNew(
    @query username: string
  ): Ok<BookingInfo> | ErrorResponse;

  /** List available slots for an online call booking. */
  @route("slots")
  @get listSlots(
    @query username: string,
    @query startDate: plainDate,
    @query endDate: plainDate,
    @query timezone: IanaTimezone
  ): Ok<TimeSlotList> | ErrorResponse;

  /** Create an online call booking. */
  @post create(@body body: CreateBookingRequest): Created<Booking> | ErrorResponse;

  /** Read a booking. */
  @get read(@path bookingId: Identifier): Ok<Booking> | ErrorResponse;

  /** Cancel a booking. */
  @route("{bookingId}/cancel")
  @post cancel(
    @path bookingId: Identifier,
    @body body: CancelBookingRequest
  ): Ok<Booking> | ErrorResponse;

  /** Reschedule a booking. */
  @route("{bookingId}/reschedule")
  @post reschedule(
    @path bookingId: Identifier,
    @body body: RescheduleBookingRequest
  ): Ok<Booking> | ErrorResponse;
}
