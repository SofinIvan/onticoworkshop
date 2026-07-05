export type BookingStatus = "confirmed" | "cancelled" | "rescheduled";

export type BookingProfile = {
  id: string;
  username: string;
  displayName: string;
  timezone: string;
  bio?: string;
  avatarUrl?: string;
};

export type MeetingTimeRule = {
  weekday: "monday" | "tuesday" | "wednesday" | "thursday" | "friday" | "saturday" | "sunday";
  startTime: string;
  endTime: string;
};

export type Meeting = {
  id: string;
  organizerId: string;
  uuid: string;
  title: string;
  description?: string;
  durationMinutes: number;
  timezone: string;
  isActive: boolean;
  minimumNoticeMinutes: number;
  slotIntervalMinutes: number;
  bufferBeforeMinutes: number;
  bufferAfterMinutes: number;
  meetingUrl?: string;
  timeRules: MeetingTimeRule[];
  createdAt: string;
  updatedAt: string;
};

export type UpdateMeetingRequest = {
  title?: string;
  description?: string;
  durationMinutes?: number;
  timezone?: string;
  isActive?: boolean;
  minimumNoticeMinutes?: number;
  slotIntervalMinutes?: number;
  bufferBeforeMinutes?: number;
  bufferAfterMinutes?: number;
  meetingUrl?: string;
  timeRules?: MeetingTimeRule[];
};

export type BookingInfo = {
  profile: BookingProfile;
  meetings: Meeting[];
};

export type TimeSlot = {
  start: string;
  end: string;
  timezone: string;
};

export type DateOverride = {
  date: string;
  isUnavailable: boolean;
  startTime?: string;
  endTime?: string;
};

export type Guest = {
  name: string;
  email: string;
  timezone: string;
};

export type Booking = {
  id: string;
  organizerId: string;
  meetingId: string;
  status: BookingStatus;
  guest: Guest;
  start: string;
  end: string;
  timezone: string;
  meetingUrl?: string;
  notes?: string;
  cancellationReason?: string;
  cancelledAt?: string;
  rescheduledFromBookingId?: string;
  createdAt: string;
  updatedAt: string;
};

export type AvailabilityRule = {
  weekday:
    | "monday"
    | "tuesday"
    | "wednesday"
    | "thursday"
    | "friday"
    | "saturday"
    | "sunday";
  startTime: string;
  endTime: string;
};

export type Availability = {
  id: string;
  organizerId: string;
  name: string;
  timezone: string;
  rules: AvailabilityRule[];
  dateOverrides: DateOverride[];
  createdAt: string;
  updatedAt: string;
};

export type UpdateAvailabilityRequest = {
  name?: string;
  timezone?: string;
  rules?: AvailabilityRule[];
  dateOverrides?: DateOverride[];
};

export type CreateBookingRequest = {
  username: string;
  meetingId?: string;
  meetingUuid?: string;
  guestName: string;
  guestEmail: string;
  guestTimezone: string;
  start: string;
  notes?: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "") ?? "";
const demoToday = new Date();
const demoDate = new Date(
  Date.UTC(demoToday.getFullYear(), demoToday.getMonth(), demoToday.getDate()),
);

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setUTCDate(date.getUTCDate() + days);
  return next;
}

function atUtcHour(date: Date, hour: number, minute = 0) {
  const next = new Date(date);
  next.setUTCHours(hour, minute, 0, 0);
  return next;
}

export const demoMeeting: Meeting = {
  id: "m_01",
  organizerId: "usr_01",
  title: "Intro call",
  uuid: "550e8400-e29b-41d4-a716-446655440000",
  description: "A focused 30 minute call to define scope and next steps.",
  durationMinutes: 30,
  timezone: "Europe/Moscow",
  isActive: true,
  minimumNoticeMinutes: 120,
  slotIntervalMinutes: 30,
  bufferBeforeMinutes: 10,
  bufferAfterMinutes: 10,
  meetingUrl: "https://meet.example.com/sofia",
  timeRules: [],
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

export const demoBookingInfo: BookingInfo = {
  profile: {
    id: "usr_01",
    username: "sofia",
    displayName: "Sofia Ivanova",
    timezone: "Europe/Moscow",
    bio: "Product strategy, calendar audits, and calm execution planning.",
  },
  meetings: [demoMeeting],
};

export const demoAvailability: Availability = {
  id: "av_01",
  organizerId: "usr_01",
  name: "Default working hours",
  timezone: "Europe/Moscow",
  rules: [
    { weekday: "monday", startTime: "09:00", endTime: "17:00" },
    { weekday: "tuesday", startTime: "09:00", endTime: "17:00" },
    { weekday: "wednesday", startTime: "10:00", endTime: "18:00" },
    { weekday: "thursday", startTime: "09:00", endTime: "17:00" },
    { weekday: "friday", startTime: "09:00", endTime: "15:00" },
  ],
  dateOverrides: [],
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

export const demoBookings: Booking[] = [
  {
    id: "bkg_1024",
    organizerId: "usr_01",
    meetingId: "m_01",
    status: "confirmed",
    guest: {
      name: "Maya Chen",
      email: "maya@example.com",
      timezone: "Europe/Berlin",
    },
    start: atUtcHour(addDays(demoDate, 1), 10).toISOString(),
    end: atUtcHour(addDays(demoDate, 1), 10, 30).toISOString(),
    timezone: "Europe/Berlin",
    meetingUrl: "https://meet.example.com/sofia",
    notes: "Review onboarding flow.",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: "bkg_1025",
    organizerId: "usr_01",
    meetingId: "m_01",
    status: "confirmed",
    guest: {
      name: "Ivan Petrov",
      email: "ivan@example.com",
      timezone: "Europe/Moscow",
    },
    start: atUtcHour(addDays(demoDate, 2), 12).toISOString(),
    end: atUtcHour(addDays(demoDate, 2), 12, 30).toISOString(),
    timezone: "Europe/Moscow",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

export function getDemoSlots(selectedDate: string, timezone: string): TimeSlot[] {
  const base = new Date(`${selectedDate}T00:00:00.000Z`);
  return [9, 9.5, 10, 11, 13, 14.5, 16].map((hour) => {
    const wholeHour = Math.floor(hour);
    const minute = hour % 1 === 0 ? 0 : 30;
    const start = atUtcHour(base, wholeHour, minute);
    const end = new Date(start);
    end.setUTCMinutes(start.getUTCMinutes() + 30);
    return {
      start: start.toISOString(),
      end: end.toISOString(),
      timezone,
    };
  });
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const fallback = await response.text();
    throw new Error(fallback || `Request failed with ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const api = {
  async readBookingInfo(username: string) {
    if (!API_BASE_URL) return demoBookingInfo;
    return request<BookingInfo>(`/booking/new?username=${encodeURIComponent(username)}`);
  },

  async listSlots(username: string, meetingId: string, selectedDate: string, timezone: string) {
    if (!API_BASE_URL) return { items: getDemoSlots(selectedDate, timezone) };
    const params = new URLSearchParams({
      username,
      meetingId,
      startDate: selectedDate,
      endDate: selectedDate,
      timezone,
    });
    return request<{ items: TimeSlot[] }>(`/booking/slots?${params.toString()}`);
  },

  async createBooking(body: CreateBookingRequest) {
    if (!API_BASE_URL) {
      return {
        ...demoBookings[0],
        id: `bkg_${Date.now()}`,
        meetingId: body.meetingId ?? demoBookings[0].meetingId,
        guest: {
          name: body.guestName,
          email: body.guestEmail,
          timezone: body.guestTimezone,
        },
        start: body.start,
        end: new Date(new Date(body.start).getTime() + 30 * 60 * 1000).toISOString(),
        timezone: body.guestTimezone,
        notes: body.notes,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
    }

    return request<Booking>("/booking", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  async listBookings(organizerId: string) {
    if (!API_BASE_URL) return { items: demoBookings };
    return request<{ items: Booking[] }>(`/booking?organizerId=${encodeURIComponent(organizerId)}`);
  },

  async listAvailabilities(organizerId: string) {
    if (!API_BASE_URL) return { items: [demoAvailability] };
    return request<{ items: Availability[] }>(
      `/availability?organizerId=${encodeURIComponent(organizerId)}`,
    );
  },

  async updateMeeting(
    meetingId: string,
    body: UpdateMeetingRequest,
  ) {
    if (!API_BASE_URL) {
      return { ...demoMeeting, ...body, id: meetingId, updatedAt: new Date().toISOString() };
    }

    return request<Meeting>(`/meeting/${encodeURIComponent(meetingId)}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    });
  },

  async updateAvailability(
    availabilityId: string,
    body: UpdateAvailabilityRequest,
  ) {
    if (!API_BASE_URL) {
      return { ...demoAvailability, ...body, id: availabilityId, updatedAt: new Date().toISOString() };
    }

    return request<Availability>(`/availability/${encodeURIComponent(availabilityId)}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    });
  },

  async deleteAvailability(availabilityId: string) {
    if (!API_BASE_URL) return;
    return request<void>(`/availability/${encodeURIComponent(availabilityId)}`, {
      method: "DELETE",
    });
  },

  async listMeetings(organizerId: string) {
    if (!API_BASE_URL) return { items: [demoMeeting] };
    return request<{ items: Meeting[] }>(
      `/meeting?organizerId=${encodeURIComponent(organizerId)}`,
    );
  },

  async createMeeting(organizerId: string, title: string, timezone: string) {
    if (!API_BASE_URL) {
      return { ...demoMeeting, id: `m_${Date.now()}` };
    }
    return request<Meeting>("/meeting", {
      method: "POST",
      body: JSON.stringify({ organizerId, title, timezone }),
    });
  },

  async deleteMeeting(meetingId: string) {
    if (!API_BASE_URL) return;
    return request<void>(`/meeting/${encodeURIComponent(meetingId)}`, {
      method: "DELETE",
    });
  },

  async getPublicMeeting(uuid: string) {
    if (!API_BASE_URL) {
      return { profile: demoBookingInfo.profile, meeting: demoMeeting };
    }
    return request<{ profile: BookingProfile; meeting: Meeting }>(
      `/public/meeting?uuid=${encodeURIComponent(uuid)}`,
    );
  },

  async listPublicSlots(uuid: string, startDate: string, endDate: string, timezone: string) {
    if (!API_BASE_URL) return { items: getDemoSlots(startDate, timezone) };
    const params = new URLSearchParams({ uuid, startDate, endDate, timezone });
    return request<{ items: TimeSlot[] }>(`/public/slots?${params.toString()}`);
  },
};
