import { type ReactNode, useEffect, useMemo, useState } from "react";
import {
  ActionIcon,
  AppShell,
  Avatar,
  Badge,
  Box,
  Button,
  Card,
  Container,
  Divider,
  Group,
  Loader,
  Modal,
  NavLink,
  Paper,
  NumberInput,
  ScrollArea,
  SimpleGrid,
  Switch,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
  ThemeIcon,
  Title,
} from "@mantine/core";
import {
  CalendarDays,
  Check,
  Clock3,
  Copy,
  Plus,
  Settings2,
  RotateCcw,
  UserRound,
  Trash2,
  Video,
  CalendarClock,
} from "lucide-react";
import {
  api,
  type Availability,
  type AvailabilityRule,
  type BookingProfile,
  type DateOverride,
  type Booking,
  type BookingInfo,
  type Meeting,
  type MeetingTimeRule,
  type TimeSlot,
} from "./api";

type ViewMode = "booking" | "workspace" | "availability";

type GuestForm = {
  name: string;
  email: string;
  notes: string;
};

const username = "sofia";
const guestTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";

function toDateInputValue(date: Date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(date.getDate() + days);
  return next;
}

function formatDayLabel(dateValue: string) {
  return new Intl.DateTimeFormat("en", {
    weekday: "short",
    month: "short",
    day: "numeric",
  }).format(new Date(`${dateValue}T12:00:00`));
}

function formatTime(dateValue: string, timezone = guestTimezone) {
  return new Intl.DateTimeFormat("en", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: timezone,
  }).format(new Date(dateValue));
}

function formatLongDate(dateValue: string, timezone = guestTimezone) {
  return new Intl.DateTimeFormat("en", {
    weekday: "long",
    month: "long",
    day: "numeric",
    timeZone: timezone,
  }).format(new Date(dateValue));
}

function initials(name?: string) {
  return name
    ?.split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

function statusColor(status: Booking["status"]) {
  if (status === "confirmed") return "green";
  if (status === "rescheduled") return "yellow";
  return "gray";
}

const viewLabels: Record<ViewMode, string> = {
  booking: "Intro call",
  workspace: "Workspace",
  availability: "Availability",
};

const weekdayOrder: AvailabilityRule["weekday"][] = [
  "monday",
  "tuesday",
  "wednesday",
  "thursday",
  "friday",
  "saturday",
  "sunday",
];

const weekdayLabels: Record<AvailabilityRule["weekday"], string> = {
  monday: "Monday",
  tuesday: "Tuesday",
  wednesday: "Wednesday",
  thursday: "Thursday",
  friday: "Friday",
  saturday: "Saturday",
  sunday: "Sunday",
};

function cloneAvailability(availability: Availability) {
  return {
    ...availability,
    rules: availability.rules.map((rule) => ({ ...rule })),
    dateOverrides: availability.dateOverrides.map((override) => ({ ...override })),
  };
}

function cloneMeeting(meeting: Meeting) {
  return { ...meeting };
}

function normalizeOptionalText(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function sortWeekdayRules(rules: AvailabilityRule[]) {
  return [...rules].sort(
    (left, right) => weekdayOrder.indexOf(left.weekday) - weekdayOrder.indexOf(right.weekday),
  );
}

function createDefaultRule(weekday: AvailabilityRule["weekday"]): AvailabilityRule {
  return {
    weekday,
    startTime: "09:00",
    endTime: "17:00",
  };
}

function createDefaultOverride(): DateOverride {
  return {
    date: toDateInputValue(addDays(new Date(), 1)),
    isUnavailable: true,
  };
}

function App() {
  const [view, setView] = useState<ViewMode>("booking");
  const [bookingInfo, setBookingInfo] = useState<BookingInfo | null>(null);
  const [meetingDraft, setMeetingDraft] = useState<Meeting | null>(null);
  const [slots, setSlots] = useState<TimeSlot[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [availabilities, setAvailabilities] = useState<Availability[]>([]);
  const [availabilityDraft, setAvailabilityDraft] = useState<Availability | null>(null);
  const [selectedDate, setSelectedDate] = useState(toDateInputValue(addDays(new Date(), 1)));
  const [selectedSlot, setSelectedSlot] = useState<TimeSlot | null>(null);
  const [selectedMeetingId, setSelectedMeetingId] = useState<string | null>(null);
  const [guest, setGuest] = useState<GuestForm>({ name: "", email: "", notes: "" });
  const [isLoading, setIsLoading] = useState(true);
  const [isSavingMeeting, setIsSavingMeeting] = useState(false);
  const [isDeletingMeeting, setIsDeletingMeeting] = useState(false);
  const [isSavingAvailability, setIsSavingAvailability] = useState(false);
  const [isDeletingSchedule, setIsDeletingSchedule] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmation, setConfirmation] = useState<Booking | null>(null);
  const [workspaceView, setWorkspaceView] = useState<"list" | "edit">("list");
  const [deleteConfirm, setDeleteConfirm] = useState<{ type: "meeting" | "availability"; id: string } | null>(null);
  const [publicMeetingUuid, setPublicMeetingUuid] = useState<string | null>(null);
  const [publicMeetingInfo, setPublicMeetingInfo] = useState<{ profile: BookingProfile; meeting: Meeting } | null>(null);
  const [publicSlots, setPublicSlots] = useState<TimeSlot[]>([]);
  const [publicGuest, setPublicGuest] = useState<GuestForm>({ name: "", email: "", notes: "" });
  const [publicBooked, setPublicBooked] = useState<{ start: string; end: string } | null>(null);
  const [publicError, setPublicError] = useState<string | null>(null);

  const dateOptions = useMemo(
    () => Array.from({ length: 7 }, (_, index) => toDateInputValue(addDays(new Date(), index + 1))),
    [],
  );

  useEffect(() => {
    let isMounted = true;

    async function loadInitialData() {
      setIsLoading(true);
      try {
        const info = await api.readBookingInfo(username);
        if (!isMounted) return;
        setBookingInfo(info);

        const [bookingResponse, availabilityResponse] = await Promise.all([
          api.listBookings(info.profile.id),
          api.listAvailabilities(info.profile.id),
        ]);

        if (!isMounted) return;
        setBookings(bookingResponse.items);
        setAvailabilities(availabilityResponse.items);
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    loadInitialData();
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    const firstAvailability = availabilities[0];
    setAvailabilityDraft(firstAvailability ? cloneAvailability(firstAvailability) : null);
  }, [availabilities]);

  useEffect(() => {
    if (!bookingInfo) return;
    setMeetingDraft(bookingInfo.meetings[0] ?? null);
    if (bookingInfo.meetings.length > 0 && !selectedMeetingId) {
      setSelectedMeetingId(bookingInfo.meetings[0].id);
    }
  }, [bookingInfo]);

  useEffect(() => {
    let isMounted = true;

    async function loadSlots() {
      if (!bookingInfo || !selectedMeetingId) return;
      const response = await api.listSlots(
        bookingInfo.profile.username,
        selectedMeetingId,
        selectedDate,
        guestTimezone,
      );
      if (isMounted) {
        setSlots(response.items);
        setSelectedSlot(response.items[0] ?? null);
      }
    }

    loadSlots();
    return () => {
      isMounted = false;
    };
  }, [bookingInfo, selectedMeetingId, selectedDate, guestTimezone]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const uuid = params.get("meeting");
    if (uuid) {
      setPublicMeetingUuid(uuid);
    }
  }, []);

  useEffect(() => {
    if (!publicMeetingUuid) return;
    api.getPublicMeeting(publicMeetingUuid).then((info) => {
      setPublicMeetingInfo(info);
    });
  }, [publicMeetingUuid]);

  useEffect(() => {
    if (!publicMeetingUuid || !publicMeetingInfo) return;
    const start = toDateInputValue(addDays(new Date(), 1));
    const end = toDateInputValue(addDays(new Date(), 7));
    api.listPublicSlots(publicMeetingUuid, start, end, guestTimezone).then((r) => {
      setPublicSlots(r.items);
      setSelectedSlot(r.items[0] ?? null);
    });
  }, [publicMeetingUuid, publicMeetingInfo]);

  function getPublicSlotsByDate(): Map<string, TimeSlot[]> {
    const map = new Map<string, TimeSlot[]>();
    for (const s of publicSlots) {
      const d = s.start.slice(0, 10);
      if (!map.has(d)) map.set(d, []);
      map.get(d)!.push(s);
    }
    return map;
  }

  function handlePublicDateSelect(date: string) {
    setSelectedDate(date);
    setPublicError(null);
    setSelectedSlot(null);
  }

  async function submitPublicBooking() {
    if (!publicMeetingUuid || !publicMeetingInfo || !selectedSlot || !publicGuest.name || !publicGuest.email) return;
    setPublicError(null);
    setIsSubmitting(true);
    try {
      await api.createBooking({
        username: publicMeetingInfo.profile.username,
        meetingUuid: publicMeetingUuid,
        guestName: publicGuest.name,
        guestEmail: publicGuest.email,
        guestTimezone,
        start: selectedSlot.start,
        notes: publicGuest.notes || undefined,
      });
      setPublicBooked({ start: selectedSlot.start, end: selectedSlot.end });
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "";
      if (msg.includes("409") || msg.includes("already registered")) {
        setPublicError("You are already registered for this meeting");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  if (publicMeetingUuid && publicMeetingInfo) {
    return (
      <PublicBookingPage
        profile={publicMeetingInfo.profile}
        meeting={publicMeetingInfo.meeting}
        slots={publicSlots}
        selectedSlot={selectedSlot}
        selectedDate={selectedDate}
        guest={publicGuest}
        publicBooked={publicBooked}
        publicError={publicError}
        isSubmitting={isSubmitting}
        dateOptions={dateOptions}
        onDateSelect={handlePublicDateSelect}
        onSlotSelect={setSelectedSlot}
        onGuestChange={setPublicGuest}
        onSubmit={submitPublicBooking}
        getSlotsByDate={getPublicSlotsByDate}
      />
    );
  }

  async function submitBooking() {
    if (!bookingInfo || !selectedSlot || !guest.name || !guest.email || !selectedMeetingId) return;
    setIsSubmitting(true);
    try {
      const booking = await api.createBooking({
        username: bookingInfo.profile.username,
        meetingId: selectedMeetingId,
        guestName: guest.name,
        guestEmail: guest.email,
        guestTimezone,
        start: selectedSlot.start,
        notes: guest.notes || undefined,
      });
      setConfirmation(booking);
      setBookings((current) => [booking, ...current]);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleCreateMeeting(draft: Meeting) {
    if (!bookingInfo) return;
    setIsSavingMeeting(true);
    try {
      const created = await api.createMeeting(bookingInfo.profile.id, draft.title, draft.timezone);
      const updated = created.id
        ? await api.updateMeeting(created.id, {
            description: normalizeOptionalText(draft.description ?? ""),
            durationMinutes: draft.durationMinutes,
            isActive: draft.isActive,
            slotIntervalMinutes: draft.slotIntervalMinutes,
            minimumNoticeMinutes: draft.minimumNoticeMinutes,
            bufferBeforeMinutes: draft.bufferBeforeMinutes,
            bufferAfterMinutes: draft.bufferAfterMinutes,
            meetingUrl: normalizeOptionalText(draft.meetingUrl ?? ""),
            timeRules: draft.timeRules,
          })
        : created;
      setBookingInfo((current) =>
        current ? { ...current, meetings: [...current.meetings, updated] } : null,
      );
      setMeetingDraft(cloneMeeting(updated));
      setWorkspaceView("list");
    } finally {
      setIsSavingMeeting(false);
    }
  }

  async function handleUpdateMeeting() {
    if (!bookingInfo || !meetingDraft) return;
    setIsSavingMeeting(true);
    try {
      const updated = await api.updateMeeting(meetingDraft.id, {
        title: meetingDraft.title.trim(),
        description: normalizeOptionalText(meetingDraft.description ?? ""),
        durationMinutes: meetingDraft.durationMinutes,
        timezone: meetingDraft.timezone.trim(),
        isActive: meetingDraft.isActive,
        minimumNoticeMinutes: meetingDraft.minimumNoticeMinutes,
        slotIntervalMinutes: meetingDraft.slotIntervalMinutes,
        bufferBeforeMinutes: meetingDraft.bufferBeforeMinutes,
        bufferAfterMinutes: meetingDraft.bufferAfterMinutes,
        meetingUrl: normalizeOptionalText(meetingDraft.meetingUrl ?? ""),
        timeRules: meetingDraft.timeRules,
      });
      setBookingInfo((current) =>
        current
          ? {
              ...current,
              meetings: current.meetings.map((m) => (m.id === updated.id ? updated : m)),
            }
          : null,
      );
      setMeetingDraft(cloneMeeting(updated));
      setWorkspaceView("list");
    } finally {
      setIsSavingMeeting(false);
    }
  }

  async function handleDeleteMeeting(meetingId: string) {
    if (!bookingInfo) return;
    setIsDeletingMeeting(true);
    try {
      await api.deleteMeeting(meetingId);
      setBookingInfo((current) =>
        current
          ? { ...current, meetings: current.meetings.filter((m) => m.id !== meetingId) }
          : null,
      );
      setBookings((current) => current.filter((b) => b.meetingId !== meetingId));
      setMeetingDraft(null);
      setWorkspaceView("list");
      setDeleteConfirm(null);
    } finally {
      setIsDeletingMeeting(false);
    }
  }

  async function handleConfirmDelete() {
    if (!deleteConfirm) return;
    try {
      if (deleteConfirm.type === "meeting") {
        handleDeleteMeeting(deleteConfirm.id);
      } else if (deleteConfirm.type === "availability") {
        await deleteAvailability();
      }
    } finally {
      setDeleteConfirm(null);
    }
  }

  async function saveAvailability() {
    if (!availabilityDraft) return;
    setIsSavingAvailability(true);
    try {
      const updated = await api.updateAvailability(availabilityDraft.id, {
        name: availabilityDraft.name.trim(),
        timezone: availabilityDraft.timezone.trim(),
        rules: availabilityDraft.rules,
        dateOverrides: availabilityDraft.dateOverrides.map((override) => ({
          date: override.date,
          isUnavailable: override.isUnavailable,
          startTime: normalizeOptionalText(override.startTime ?? ""),
          endTime: normalizeOptionalText(override.endTime ?? ""),
        })),
      });
      setAvailabilities((current) =>
        current.map((a) => (a.id === updated.id ? updated : a)),
      );
      setAvailabilityDraft(cloneAvailability(updated));
    } finally {
      setIsSavingAvailability(false);
    }
  }

  async function deleteAvailability() {
    if (!availabilityDraft) return;
    if (availabilities.length <= 1) return;

    setIsDeletingSchedule(true);
    try {
      const deletedId = availabilityDraft.id;
      await api.deleteAvailability(deletedId);
      const remaining = availabilities.filter((a) => a.id !== deletedId);
      setAvailabilities(remaining);
      setAvailabilityDraft(remaining[0] ? cloneAvailability(remaining[0]) : null);
    } finally {
      setIsDeletingSchedule(false);
    }
  }

  if (isLoading || !bookingInfo) {
    return (
      <Container h="100vh">
        <Group h="100%" justify="center">
          <Loader color="dark" />
        </Group>
      </Container>
    );
  }

  return (
    <AppShell navbar={{ width: 260, breakpoint: "sm" }} padding="md">
      <AppShell.Navbar p="md">
        <Stack h="100%">
          <Group>
            <Avatar color="dark" radius="sm">
              cb
            </Avatar>
            <Text fw={700}>Calendar Booking</Text>
          </Group>

          <Stack gap={4}>
            <NavLink
              active={view === "booking"}
              label="Booking"
              leftSection={<CalendarDays size={18} />}
              onClick={() => setView("booking")}
            />
            <NavLink
              active={view === "workspace"}
              label="Workspace"
              leftSection={<Settings2 size={18} />}
              onClick={() => setView("workspace")}
            />
            <NavLink
              active={view === "availability"}
              label="Availability"
              leftSection={<CalendarClock size={18} />}
              onClick={() => setView("availability")}
            />
          </Stack>

          <Box mt="auto">
            <Group gap="sm" wrap="nowrap">
              <Avatar src={bookingInfo.profile.avatarUrl} color="dark" radius="xl">
                {initials(bookingInfo.profile.displayName)}
              </Avatar>
              <Box>
                <Text size="sm" fw={600}>
                  {bookingInfo.profile.displayName}
                </Text>
                <Text size="xs" c="dimmed">
                  /{bookingInfo.profile.username}
                </Text>
              </Box>
            </Group>
          </Box>
        </Stack>
      </AppShell.Navbar>

      <AppShell.Main>
        <Container size="xl">
          <Group justify="space-between" align="flex-start" mb="lg">
            <Box>
              <Title order={1}>{viewLabels[view]}</Title>
              <Text c="dimmed" size="sm">
                {(() => {
                  const sm = bookingInfo.meetings.find((m) => m.id === selectedMeetingId);
                  return sm ? `${sm.title} · ${sm.durationMinutes} min · ${sm.timezone}` : "";
                })()}
              </Text>
            </Box>
          </Group>

          {view === "booking" ? (
            <BookingView
              bookingInfo={bookingInfo}
              dateOptions={dateOptions}
              selectedDate={selectedDate}
              selectedSlot={selectedSlot}
              selectedMeetingId={selectedMeetingId}
              slots={slots}
              bookings={bookings}
              guest={guest}
              confirmation={confirmation}
              isSubmitting={isSubmitting}
              onDateChange={(value) => {
                setSelectedDate(value);
                setConfirmation(null);
              }}
              onSlotChange={(slot) => {
                setSelectedSlot(slot);
                setConfirmation(null);
              }}
              onSelectMeeting={(id) => {
                setSelectedMeetingId(id);
                setSelectedSlot(null);
                setConfirmation(null);
              }}
              onGuestChange={setGuest}
              onSubmit={submitBooking}
            />
          ) : view === "workspace" ? (
            <WorkspaceView
              meetings={bookingInfo.meetings}
              bookings={bookings}
              meetingDraft={meetingDraft}
              workspaceView={workspaceView}
              isSaving={isSavingMeeting}
              isDeleting={isDeletingMeeting}
              onCancel={() => {
                setWorkspaceView("list");
                setMeetingDraft(null);
              }}
              onEdit={(meeting) => {
                setMeetingDraft(cloneMeeting(meeting));
                setWorkspaceView("edit");
              }}
              onCreateNew={() => {
                setMeetingDraft({
                  id: "",
                  organizerId: bookingInfo.profile.id,
                  uuid: "",
                  title: "",
                  description: "",
                  durationMinutes: 30,
                  timezone: bookingInfo.profile.timezone,
                  isActive: true,
                  slotIntervalMinutes: 30,
                  minimumNoticeMinutes: 120,
                  bufferBeforeMinutes: 10,
                  bufferAfterMinutes: 10,
                  meetingUrl: "",
                  timeRules: [],
                  createdAt: "",
                  updatedAt: "",
                });
                setWorkspaceView("edit");
              }}
              onDraftChange={(nextDraft) => setMeetingDraft(nextDraft)}
              onSave={() => {
                if (meetingDraft?.id) {
                  handleUpdateMeeting();
                } else if (meetingDraft) {
                  handleCreateMeeting(meetingDraft);
                }
              }}
              onRequestDelete={(id) => setDeleteConfirm({ type: "meeting", id })}
            />
          ) : view === "availability" ? (
            <AvailabilityView
              draft={availabilityDraft}
              isDeleting={isDeletingSchedule}
              isSaving={isSavingAvailability}
              canDelete={availabilities.length > 1}
              onDraftChange={(nextDraft) => setAvailabilityDraft(nextDraft)}
              onDelete={() => setDeleteConfirm({ type: "availability", id: availabilityDraft?.id ?? "" })}
              onReset={() => {
                const firstAvailability = availabilities[0];
                setAvailabilityDraft(firstAvailability ? cloneAvailability(firstAvailability) : null);
              }}
              onSave={saveAvailability}
            />
          ) : null}
        </Container>
      </AppShell.Main>

      <Modal
        opened={deleteConfirm !== null}
        onClose={() => setDeleteConfirm(null)}
        title="Confirm deletion"
        centered
      >
        <Text mb="lg">
          Are you sure you want to delete this {deleteConfirm?.type === "meeting" ? "meeting" : "availability"}?
          {deleteConfirm?.type === "meeting" ? " All bookings for this meeting will also be deleted." : ""}
        </Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={() => setDeleteConfirm(null)}>Cancel</Button>
          <Button color="red" onClick={handleConfirmDelete}>Delete</Button>
        </Group>
      </Modal>
    </AppShell>
  );
}

type BookingViewProps = {
  bookingInfo: BookingInfo;
  dateOptions: string[];
  selectedDate: string;
  selectedSlot: TimeSlot | null;
  selectedMeetingId: string | null;
  slots: TimeSlot[];
  bookings: Booking[];
  guest: GuestForm;
  confirmation: Booking | null;
  isSubmitting: boolean;
  onDateChange: (value: string) => void;
  onSlotChange: (slot: TimeSlot) => void;
  onSelectMeeting: (id: string) => void;
  onGuestChange: (guest: GuestForm) => void;
  onSubmit: () => void;
};

function BookingView({
  bookingInfo,
  dateOptions,
  selectedDate,
  selectedSlot,
  selectedMeetingId,
  slots,
  bookings,
  guest,
  confirmation,
  isSubmitting,
  onDateChange,
  onSlotChange,
  onSelectMeeting,
  onGuestChange,
  onSubmit,
}: BookingViewProps) {
  const canSubmit = Boolean(selectedSlot && guest.name && guest.email);
  const selectedMeeting = bookingInfo.meetings.find((m) => m.id === selectedMeetingId);
  const activeMeetings = bookingInfo.meetings.filter((m) => m.isActive);

  if (!selectedMeetingId) {
    return (
      <Stack>
        <Card withBorder>
          <Stack p="md">
            <Group gap="sm">
              <Avatar size={48} src={bookingInfo.profile.avatarUrl} radius="xl" color="dark">
                {initials(bookingInfo.profile.displayName)}
              </Avatar>
              <Box>
                <Text fw={650}>{bookingInfo.profile.displayName}</Text>
                <Text size="sm" c="dimmed">
                  {bookingInfo.profile.bio}
                </Text>
              </Box>
            </Group>
          </Stack>
        </Card>

        <Text fw={600}>Select a meeting</Text>
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          {activeMeetings.map((m) => (
            <Card
              key={m.id}
              withBorder
              padding="lg"
              style={{ cursor: "pointer" }}
              onClick={() => onSelectMeeting(m.id)}
            >
              <Group justify="space-between" mb="xs">
                <Text fw={600}>{m.title}</Text>
                <Badge color="green" variant="light">{m.durationMinutes}m</Badge>
              </Group>
              {m.timeRules.length > 0 && (
                <Text size="xs" c="dimmed" mb="xs">
                  {m.timeRules.map((r) => `${r.weekday.slice(0, 3)} ${r.startTime.slice(0, 5)}-${r.endTime.slice(0, 5)}`).join(" · ")}
                </Text>
              )}
              <Text size="sm" c="dimmed" lineClamp={2} mb="md">
                {m.description || "Online meeting"}
              </Text>
              {m.uuid && (
                <Button
                  variant="light"
                  size="xs"
                  leftSection={<Copy size={14} />}
                  onClick={(e) => {
                    e.stopPropagation();
                    navigator.clipboard.writeText(`${window.location.origin}/?meeting=${m.uuid}`);
                  }}
                >
                  Copy link
                </Button>
              )}
            </Card>
          ))}
        </SimpleGrid>

        <BookingsView bookings={bookings} meetings={bookingInfo.meetings} />
      </Stack>
    );
  }

  return (
    <Stack>
      <Group gap="xs">
        {bookingInfo.meetings
          .filter((m) => m.isActive)
          .map((m) => (
            <Badge
              key={m.id}
              size="lg"
              variant={m.id === selectedMeetingId ? "filled" : "outline"}
              color="dark"
              style={{ cursor: "pointer" }}
              onClick={() => onSelectMeeting(m.id)}
            >
              {m.title} ({m.durationMinutes}m)
            </Badge>
          ))}
      </Group>
      <Card withBorder>
        <SimpleGrid cols={{ base: 1, md: 3 }} spacing={0}>
          <Stack p="md">
            <Group gap="sm">
              <Avatar size={48} src={bookingInfo.profile.avatarUrl} radius="xl" color="dark">
                {initials(bookingInfo.profile.displayName)}
              </Avatar>
              <Box>
                <Text fw={650}>{bookingInfo.profile.displayName}</Text>
                <Text size="sm" c="dimmed">
                  {bookingInfo.profile.bio}
                </Text>
              </Box>
            </Group>

            <Divider />

            <Stack gap="md">
              <InfoRow icon={<Clock3 size={16} />} text={`${selectedMeeting?.durationMinutes ?? 30} minutes`} />
              <InfoRow icon={<Video size={16} />} text={selectedMeeting?.title ?? ""} />
              <InfoRow icon={<UserRound size={16} />} text={guestTimezone} />
              <Button variant="subtle" size="xs" onClick={() => onSelectMeeting("")} pl={0}>
                ← Choose another meeting
              </Button>
            </Stack>
          </Stack>

          <Stack p="md">
            <Box>
              <Title order={2}>Select a time</Title>
              <Text c="dimmed" size="sm">
                {selectedSlot ? formatLongDate(selectedSlot.start, guestTimezone) : formatDayLabel(selectedDate)}
              </Text>
            </Box>

            <SimpleGrid cols={{ base: 2, sm: 4, md: 7 }} spacing="xs">
              {dateOptions.map((dateValue) => (
                <Button
                  key={dateValue}
                  size="sm"
                  variant={dateValue === selectedDate ? "filled" : "default"}
                  color="dark"
                  onClick={() => onDateChange(dateValue)}
                >
                  {formatDayLabel(dateValue)}
                </Button>
              ))}
            </SimpleGrid>

            <ScrollArea h={330} offsetScrollbars>
              <Stack gap={4}>
                {slots.map((slot) => (
                  <Paper
                    key={slot.start}
                    withBorder
                    p="sm"
                    style={{
                      cursor: "pointer",
                      backgroundColor: selectedSlot?.start === slot.start ? "var(--mantine-color-dark-1)" : undefined,
                    }}
                    onClick={() => onSlotChange(slot)}
                  >
                    <Group justify="space-between">
                      <Text size="sm" fw={selectedSlot?.start === slot.start ? 700 : 400} c={selectedSlot?.start === slot.start ? "white" : undefined}>
                        {formatTime(slot.start, guestTimezone)} — {formatTime(slot.end, guestTimezone)}
                      </Text>
                      <Badge variant="light" size="sm" color={selectedSlot?.start === slot.start ? "white" : "gray"}>
                        {(() => {
                          const m = bookingInfo.meetings.find((x) => x.id === selectedMeetingId);
                          return `${m?.durationMinutes ?? 30}m`;
                        })()}
                      </Badge>
                    </Group>
                  </Paper>
                ))}
              </Stack>
            </ScrollArea>
          </Stack>

          <Stack p="md">
            <Box>
              <Title order={2}>Your details</Title>
              <Text c="dimmed" size="sm">
                {selectedSlot
                  ? `${formatTime(selectedSlot.start)} - ${formatTime(selectedSlot.end)}`
                  : "Choose a slot"}
              </Text>
            </Box>

            <TextInput
              label="Name"
              value={guest.name}
              onChange={(event) => onGuestChange({ ...guest, name: event.currentTarget.value })}
            />
            <TextInput
              label="Email"
              type="email"
              value={guest.email}
              onChange={(event) => onGuestChange({ ...guest, email: event.currentTarget.value })}
            />
            <Textarea
              label="Notes"
              minRows={4}
              value={guest.notes}
              onChange={(event) => onGuestChange({ ...guest, notes: event.currentTarget.value })}
            />

            <Button
              color="dark"
              leftSection={confirmation ? <Check size={16} /> : <CalendarDays size={16} />}
              loading={isSubmitting}
              disabled={!canSubmit}
              onClick={onSubmit}
            >
              {confirmation ? "Booked" : "Confirm"}
            </Button>

            {confirmation ? (
              <Paper withBorder p="sm">
                <Group gap="sm" wrap="nowrap">
                  <ThemeIcon color="green" variant="light">
                    <Check size={16} />
                  </ThemeIcon>
                  <Box>
                    <Text size="sm" fw={600}>
                      {formatLongDate(confirmation.start)}
                    </Text>
                    <Text size="xs" c="dimmed">
                      {formatTime(confirmation.start)} · {confirmation.guest.email}
                    </Text>
                  </Box>
                </Group>
              </Paper>
            ) : null}
          </Stack>
        </SimpleGrid>
      </Card>

      <BookingsView bookings={bookings} meetings={bookingInfo.meetings} />
    </Stack>
  );
}

type WorkspaceViewProps = {
  meetings: Meeting[];
  bookings: Booking[];
  meetingDraft: Meeting | null;
  workspaceView: "list" | "edit";
  isSaving: boolean;
  isDeleting: boolean;
  onCancel: () => void;
  onEdit: (meeting: Meeting) => void;
  onCreateNew: () => void;
  onDraftChange: (draft: Meeting) => void;
  onSave: () => void;
  onRequestDelete: (id: string) => void;
};

function WorkspaceView({
  meetings,
  bookings,
  meetingDraft,
  workspaceView,
  isSaving,
  isDeleting,
  onCancel,
  onEdit,
  onCreateNew,
  onDraftChange,
  onSave,
  onRequestDelete,
}: WorkspaceViewProps) {
  if (workspaceView === "edit" && meetingDraft) {
    const settings = meetingDraft;
    const canSave = Boolean(settings.title.trim() && settings.timezone.trim());
    const isNew = !settings.id;

    return (
      <Card withBorder>
      <Stack gap="lg">
        <Group justify="space-between" align="flex-start">
          <Box>
            <Title order={2}>{isNew ? "New meeting" : settings.title}</Title>
          </Box>
          <Badge color={settings.isActive ? "green" : "gray"} variant="light">
            {settings.isActive ? "Active" : "Paused"}
          </Badge>
        </Group>

        <SimpleGrid cols={{ base: 1, lg: 2 }} spacing="lg">
          <Stack gap="md">
            <TextInput
              label="Title"
              value={settings.title}
              onChange={(event) => onDraftChange({ ...settings, title: event.currentTarget.value })}
            />
            <Textarea
              label="Description"
              minRows={3}
              value={settings.description ?? ""}
              onChange={(event) => onDraftChange({ ...settings, description: event.currentTarget.value })}
            />
            <TextInput
              label="Meeting URL"
              value={settings.meetingUrl ?? ""}
              onChange={(event) => onDraftChange({ ...settings, meetingUrl: event.currentTarget.value })}
            />
            <TextInput
              label="Timezone"
              value={settings.timezone}
              onChange={(event) => onDraftChange({ ...settings, timezone: event.currentTarget.value })}
            />
            <Switch
              label="Active"
              checked={settings.isActive}
              onChange={(event) => onDraftChange({ ...settings, isActive: event.currentTarget.checked })}
            />
          </Stack>

          <Stack gap="md">
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              <NumberInput
                label="Duration"
                min={5} step={5}
                value={settings.durationMinutes}
                onChange={(value) => onDraftChange({ ...settings, durationMinutes: typeof value === "number" ? value : settings.durationMinutes })}
              />
              <NumberInput
                label="Minimum notice"
                min={0} step={15}
                value={settings.minimumNoticeMinutes}
                onChange={(value) => onDraftChange({ ...settings, minimumNoticeMinutes: typeof value === "number" ? value : settings.minimumNoticeMinutes })}
              />
              <NumberInput
                label="Slot interval"
                min={5} step={5}
                value={settings.slotIntervalMinutes}
                onChange={(value) => onDraftChange({ ...settings, slotIntervalMinutes: typeof value === "number" ? value : settings.slotIntervalMinutes })}
              />
              <NumberInput
                label="Buffer before"
                min={0} step={5}
                value={settings.bufferBeforeMinutes}
                onChange={(value) => onDraftChange({ ...settings, bufferBeforeMinutes: typeof value === "number" ? value : settings.bufferBeforeMinutes })}
              />
              <NumberInput
                label="Buffer after"
                min={0} step={5}
                value={settings.bufferAfterMinutes}
                onChange={(value) => onDraftChange({ ...settings, bufferAfterMinutes: typeof value === "number" ? value : settings.bufferAfterMinutes })}
              />
            </SimpleGrid>

            <Box>
              <Group justify="space-between" align="center" mb="xs">
                <Box>
                  <Text fw={600}>Time restrictions</Text>
                  <Text size="xs" c="dimmed">
                    Limit this meeting to specific weekdays and hours. Leave empty to use full availability.
                  </Text>
                </Box>
              </Group>

              <Group gap="xs" align="flex-end">
                <Select
                  placeholder="Pick a day"
                  data={weekdayOrder
                    .filter((w) => !(settings.timeRules ?? []).some((r) => r.weekday === w))
                    .map((w) => ({ value: w, label: weekdayLabels[w] }))
                  }
                  value={null}
                  onChange={(value) => {
                    if (value) {
                      onDraftChange({
                        ...settings,
                        timeRules: [...(settings.timeRules ?? []), { weekday: value as MeetingTimeRule["weekday"], startTime: "09:00", endTime: "17:00" }],
                      });
                    }
                  }}
                  disabled={(settings.timeRules ?? []).length >= 7}
                  clearable={false}
                  w={160}
                />
              </Group>

              <Stack gap="xs">
                {weekdayOrder.map((weekday) => {
                  const rule = (settings.timeRules ?? []).find((r) => r.weekday === weekday);
                  if (!rule) return null;

                  return (
                    <Paper key={weekday} withBorder p="sm">
                      <Group align="flex-end" justify="space-between" wrap="nowrap">
                        <Text size="sm" w={110} tt="capitalize">
                          {weekdayLabels[weekday]}
                        </Text>
                        <TextInput
                          label="Start"
                          type="time"
                          value={rule.startTime}
                          onChange={(event) =>
                            onDraftChange({
                              ...settings,
                              timeRules: (settings.timeRules ?? []).map((r) =>
                                r.weekday === weekday ? { ...r, startTime: event.currentTarget.value } : r,
                              ),
                            })
                          }
                        />
                        <TextInput
                          label="End"
                          type="time"
                          value={rule.endTime}
                          onChange={(event) =>
                            onDraftChange({
                              ...settings,
                              timeRules: (settings.timeRules ?? []).map((r) =>
                                r.weekday === weekday ? { ...r, endTime: event.currentTarget.value } : r,
                              ),
                            })
                          }
                        />
                        <ActionIcon
                          variant="default"
                          aria-label={`Remove ${weekdayLabels[weekday]} rule`}
                          onClick={() =>
                            onDraftChange({
                              ...settings,
                              timeRules: (settings.timeRules ?? []).filter((r) => r.weekday !== weekday),
                            })
                          }
                        >
                          <Trash2 size={16} />
                        </ActionIcon>
                      </Group>
                    </Paper>
                  );
                })}
              </Stack>
            </Box>

            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              <Metric icon={<Clock3 size={16} />} label="Duration" value={`${settings.durationMinutes}m`} />
              <Metric icon={<CalendarDays size={16} />} label="Notice" value={`${settings.minimumNoticeMinutes}m`} />
              <Metric icon={<Settings2 size={16} />} label="Interval" value={`${settings.slotIntervalMinutes}m`} />
              <Metric icon={<Video size={16} />} label="Buffer" value={`${settings.bufferBeforeMinutes}/${settings.bufferAfterMinutes}m`} />
            </SimpleGrid>
          </Stack>
        </SimpleGrid>

        <Group justify="space-between" align="center" wrap="nowrap">
          <Group>
            {!isNew && (
              <Button variant="light" color="red" leftSection={<Trash2 size={16} />} loading={isDeleting} onClick={() => onRequestDelete(settings.id)}>
                Delete
              </Button>
            )}
          </Group>
          <Group>
            <Button variant="default" onClick={onCancel}>Cancel</Button>
            <Button color="dark" loading={isSaving} disabled={!canSave} onClick={onSave}>
              {isNew ? "Create" : "Save changes"}
            </Button>
          </Group>
        </Group>
      </Stack>
      </Card>
    );
  }

  return (
    <Stack>
      <Group justify="space-between" align="center">
        <Title order={2}>Meetings</Title>
        <Button leftSection={<Plus size={16} />} onClick={onCreateNew}>
          New meeting
        </Button>
      </Group>

      {meetings.length === 0 ? (
        <Card withBorder>
          <Text c="dimmed">No meetings yet. Create your first one.</Text>
        </Card>
      ) : (
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          {meetings.map((m) => (
            <Card key={m.id} withBorder padding="lg">
              <Group justify="space-between" mb="xs">
                <Text fw={600}>{m.title}</Text>
                <Badge color={m.isActive ? "green" : "gray"} variant="light">
                  {m.isActive ? "Active" : "Paused"}
                </Badge>
              </Group>
              <Text size="sm" c="dimmed" mb="xs">
                {m.durationMinutes} min · {m.timezone}
              </Text>
              {m.timeRules.length > 0 && (
                <Text size="xs" c="dimmed" mb="xs">
                  {m.timeRules.map((r) => `${r.weekday.slice(0, 3)} ${r.startTime.slice(0, 5)}-${r.endTime.slice(0, 5)}`).join(" · ")}
                </Text>
              )}
              <Text size="sm" c="dimmed" lineClamp={2} mb="md">
                {m.description || "No description"}
              </Text>
              {(() => {
                const meetingBookings = bookings.filter((b) => b.meetingId === m.id);
                return meetingBookings.length > 0 ? (
                  <Text size="xs" c="dimmed" mb="xs">
                    {meetingBookings.length} participant{meetingBookings.length > 1 ? "s" : ""}: {meetingBookings.map((b) => b.guest.name).join(", ")}
                  </Text>
                ) : null;
              })()}
              <Group>
                {m.uuid && (
                  <Button
                    variant="light"
                    size="xs"
                    leftSection={<Copy size={14} />}
                    onClick={() => {
                      navigator.clipboard.writeText(`${window.location.origin}/?meeting=${m.uuid}`);
                    }}
                  >
                    Copy link
                  </Button>
                )}
                <Button variant="light" size="xs" onClick={() => onEdit(m)}>Edit</Button>
                <Button variant="light" color="red" size="xs" onClick={() => onRequestDelete(m.id)}>Delete</Button>
              </Group>
            </Card>
          ))}
        </SimpleGrid>
      )}
    </Stack>
  );
}

type AvailabilityViewProps = {
  draft: Availability | null;
  isSaving: boolean;
  isDeleting: boolean;
  canDelete: boolean;
  onDraftChange: (draft: Availability) => void;
  onDelete: () => void;
  onReset: () => void;
  onSave: () => void;
};

function AvailabilityView({
  draft,
  isSaving,
  isDeleting,
  canDelete,
  onDraftChange,
  onDelete,
  onReset,
  onSave,
}: AvailabilityViewProps) {
  const activeSchedule = draft;
  const nextRuleWeekday = activeSchedule
    ? weekdayOrder.find((weekday) => !activeSchedule.rules.some((rule) => rule.weekday === weekday))
    : undefined;

  function updateRule(
    weekday: AvailabilityRule["weekday"],
    field: "startTime" | "endTime",
    value: string,
  ) {
    if (!activeSchedule) return;
    onDraftChange({
      ...activeSchedule,
      rules: activeSchedule.rules.map((rule) =>
        rule.weekday === weekday ? { ...rule, [field]: value } : rule,
      ),
    });
  }

  function addRule() {
    if (!activeSchedule || !nextRuleWeekday) return;
    onDraftChange({
      ...activeSchedule,
      rules: sortWeekdayRules([...activeSchedule.rules, createDefaultRule(nextRuleWeekday)]),
    });
  }

  function removeRule(weekday: AvailabilityRule["weekday"]) {
    if (!activeSchedule) return;
    onDraftChange({
      ...activeSchedule,
      rules: activeSchedule.rules.filter((rule) => rule.weekday !== weekday),
    });
  }

  function addOverride() {
    if (!activeSchedule) return;
    onDraftChange({
      ...activeSchedule,
      dateOverrides: [...activeSchedule.dateOverrides, createDefaultOverride()],
    });
  }

  function updateOverride(index: number, patch: Partial<DateOverride>) {
    if (!activeSchedule) return;
    onDraftChange({
      ...activeSchedule,
      dateOverrides: activeSchedule.dateOverrides.map((override, currentIndex) =>
        currentIndex === index ? { ...override, ...patch } : override,
      ),
    });
  }

  function removeOverride(index: number) {
    if (!activeSchedule) return;
    onDraftChange({
      ...activeSchedule,
      dateOverrides: activeSchedule.dateOverrides.filter((_, currentIndex) => currentIndex !== index),
    });
  }

  if (!activeSchedule) {
    return (
      <Card withBorder>
        <Text c="dimmed">No availability schedule is loaded.</Text>
      </Card>
    );
  }

  return (
    <Card withBorder>
      <Stack gap="lg">
        <Group justify="space-between" align="flex-start">
          <Box>
            <Title order={2}>Availability</Title>
            <Text size="sm" c="dimmed">
              {activeSchedule?.timezone}
            </Text>
          </Box>
          <Group>
            <Button variant="default" leftSection={<RotateCcw size={16} />} onClick={onReset}>
              Reset
            </Button>
            <Button color="dark" loading={isSaving} onClick={onSave}>
              Save changes
            </Button>
          </Group>
        </Group>

        <SimpleGrid cols={{ base: 1, lg: 2 }} spacing="lg">
          <Stack gap="md">
            <TextInput
              label="Schedule name"
              value={activeSchedule.name}
              onChange={(event) => onDraftChange({ ...activeSchedule, name: event.currentTarget.value })}
            />
            <TextInput
              label="Timezone"
              value={activeSchedule.timezone}
              onChange={(event) =>
                onDraftChange({ ...activeSchedule, timezone: event.currentTarget.value })
              }
            />

            <Group justify="space-between" align="center">
              <Box>
                <Text fw={600}>Weekly rules</Text>
                <Text size="xs" c="dimmed">
                  Matches the API `rules` array.
                </Text>
              </Box>
              <Button
                variant="light"
                leftSection={<Plus size={16} />}
                onClick={addRule}
                disabled={!nextRuleWeekday}
              >
                Add rule
              </Button>
            </Group>

            <Stack gap="xs">
              {weekdayOrder.map((weekday) => {
                const rule = activeSchedule.rules.find((entry) => entry.weekday === weekday);

                if (!rule) return null;

                return (
                  <Paper key={weekday} withBorder p="sm">
                    <Group align="flex-end" justify="space-between" wrap="nowrap">
                      <Text size="sm" w={110} tt="capitalize">
                        {weekdayLabels[weekday]}
                      </Text>
                      <TextInput
                        label="Start"
                        type="time"
                        value={rule.startTime}
                        onChange={(event) => updateRule(weekday, "startTime", event.currentTarget.value)}
                      />
                      <TextInput
                        label="End"
                        type="time"
                        value={rule.endTime}
                        onChange={(event) => updateRule(weekday, "endTime", event.currentTarget.value)}
                      />
                      <ActionIcon
                        variant="default"
                        aria-label={`Remove ${weekdayLabels[weekday]} rule`}
                        onClick={() => removeRule(weekday)}
                      >
                        <Trash2 size={16} />
                      </ActionIcon>
                    </Group>
                  </Paper>
                );
              })}
            </Stack>
          </Stack>

          <Stack gap="md">
            <Group justify="space-between" align="center">
              <Box>
                <Text fw={600}>Date overrides</Text>
                <Text size="xs" c="dimmed">
                  Matches the API `dateOverrides` array.
                </Text>
              </Box>
              <Button variant="light" leftSection={<Plus size={16} />} onClick={addOverride}>
                Add override
              </Button>
            </Group>

            <Stack gap="xs">
              {activeSchedule.dateOverrides.length ? (
                activeSchedule.dateOverrides.map((override, index) => (
                  <Paper key={`${override.date}-${index}`} withBorder p="sm">
                    <Stack gap="sm">
                      <Group align="flex-end" wrap="nowrap">
                        <TextInput
                          label="Date"
                          type="date"
                          value={override.date}
                          onChange={(event) =>
                            updateOverride(index, { date: event.currentTarget.value })
                          }
                        />
                        <Switch
                          label="Unavailable"
                          checked={override.isUnavailable}
                          onChange={(event) =>
                            updateOverride(index, { isUnavailable: event.currentTarget.checked })
                          }
                        />
                        <ActionIcon
                          variant="default"
                          aria-label="Remove override"
                          onClick={() => removeOverride(index)}
                        >
                          <Trash2 size={16} />
                        </ActionIcon>
                      </Group>
                      <SimpleGrid cols={{ base: 1, sm: 2 }}>
                        <TextInput
                          label="Start"
                          type="time"
                          value={override.startTime ?? ""}
                          disabled={override.isUnavailable}
                          onChange={(event) =>
                            updateOverride(index, { startTime: event.currentTarget.value })
                          }
                        />
                        <TextInput
                          label="End"
                          type="time"
                          value={override.endTime ?? ""}
                          disabled={override.isUnavailable}
                          onChange={(event) =>
                            updateOverride(index, { endTime: event.currentTarget.value })
                          }
                        />
                      </SimpleGrid>
                    </Stack>
                  </Paper>
                ))
              ) : (
                <Paper withBorder p="md">
                  <Text size="sm" c="dimmed">
                    No date overrides yet.
                  </Text>
                </Paper>
              )}
            </Stack>

            <Group justify="space-between" align="center">
              <Text size="sm" c="dimmed">
                Update or remove the current schedule through the availability-schedule API.
              </Text>
              <Button
                variant="light"
                color="red"
                leftSection={<Trash2 size={16} />}
                disabled={!canDelete}
                loading={isDeleting}
                onClick={onDelete}
              >
                Delete schedule
              </Button>
            </Group>
          </Stack>
        </SimpleGrid>

        <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
          <Metric icon={<CalendarClock size={16} />} label="Rules" value={`${activeSchedule.rules.length}`} />
          <Metric icon={<CalendarDays size={16} />} label="Overrides" value={`${activeSchedule.dateOverrides.length}`} />
          <Metric icon={<Settings2 size={16} />} label="Name" value={activeSchedule.name} />
          <Metric icon={<Clock3 size={16} />} label="Timezone" value={activeSchedule.timezone} />
        </SimpleGrid>
      </Stack>
    </Card>
  );
}

type BookingsViewProps = {
  bookings: Booking[];
  meetings: Meeting[];
};

function BookingsView({ bookings, meetings }: BookingsViewProps) {
  const [filterMeetingId, setFilterMeetingId] = useState<string | null>(null);
  const [filterGuestEmail, setFilterGuestEmail] = useState<string | null>(null);
  const [sortField, setSortField] = useState<"guest" | "meeting" | "time" | "status">("time");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("asc");

  function getMeetingTitle(meetingId: string) {
    return meetings.find((m) => m.id === meetingId)?.title ?? meetingId;
  }

  const guestOptions = Array.from(
    new Map(bookings.map((b) => [b.guest.email, { value: b.guest.email, label: `${b.guest.name} (${b.guest.email})` }])).values()
  );

  let filtered = bookings;
  if (filterMeetingId) filtered = filtered.filter((b) => b.meetingId === filterMeetingId);
  if (filterGuestEmail) filtered = filtered.filter((b) => b.guest.email === filterGuestEmail);
  const dir = sortDir === "asc" ? 1 : -1;
  filtered = [...filtered].sort((a, b) => {
    switch (sortField) {
      case "guest": return dir * a.guest.name.localeCompare(b.guest.name);
      case "meeting": return dir * getMeetingTitle(a.meetingId).localeCompare(getMeetingTitle(b.meetingId));
      case "time": return dir * (new Date(a.start).getTime() - new Date(b.start).getTime());
      case "status": return dir * a.status.localeCompare(b.status);
      default: return 0;
    }
  });

  function toggleSort(field: typeof sortField) {
    if (sortField === field) {
      setSortDir(sortDir === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDir("asc");
    }
  }

  function sortIndicator(field: typeof sortField) {
    if (sortField !== field) return "";
    return sortDir === "asc" ? " ▲" : " ▼";
  }

  const meetingOptions = meetings.map((m) => ({ value: m.id, label: m.title }));

  return (
    <Card withBorder>
      <Group justify="space-between" mb="md">
        <Title order={2}>Bookings</Title>
        <Badge variant="outline" color="gray">
          {filtered.length} / {bookings.length}
        </Badge>
      </Group>

      <Group mb="md" gap="sm">
        <Select
          placeholder="All meetings"
          data={meetingOptions}
          value={filterMeetingId}
          onChange={(_v) => setFilterMeetingId(_v)}
          allowDeselect
          searchable
          w={200}
        />
        <Select
          placeholder="All guests"
          data={guestOptions}
          value={filterGuestEmail}
          onChange={(_v) => setFilterGuestEmail(_v)}
          allowDeselect
          searchable
          w={260}
        />
      </Group>

      <Table.ScrollContainer minWidth={640}>
        {filtered.length === 0 ? (
          <Text c="dimmed" py="md" ta="center">
            {bookings.length === 0 ? "No bookings yet" : "No bookings match your filters"}
          </Text>
        ) : (
        <Table verticalSpacing="md" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th style={{ cursor: "pointer" }} onClick={() => toggleSort("guest")}>
                Guest{sortIndicator("guest")}
              </Table.Th>
              <Table.Th style={{ cursor: "pointer" }} onClick={() => toggleSort("meeting")}>
                Meeting{sortIndicator("meeting")}
              </Table.Th>
              <Table.Th style={{ cursor: "pointer" }} onClick={() => toggleSort("time")}>
                Time{sortIndicator("time")}
              </Table.Th>
              <Table.Th style={{ cursor: "pointer" }} onClick={() => toggleSort("status")}>
                Status{sortIndicator("status")}
              </Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {filtered.map((booking) => (
              <Table.Tr key={booking.id}>
                <Table.Td>
                  <Text size="sm" fw={600}>
                    {booking.guest.name}
                  </Text>
                  <Text size="xs" c="dimmed">
                    {booking.guest.email}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{getMeetingTitle(booking.meetingId)}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{formatLongDate(booking.start, booking.timezone)}</Text>
                  <Text size="xs" c="dimmed">
                    {formatTime(booking.start, booking.timezone)}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Badge color={statusColor(booking.status)} variant="light">
                    {booking.status}
                  </Badge>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
        )}
      </Table.ScrollContainer>
    </Card>
  );
}

type InfoRowProps = {
  icon: ReactNode;
  text: string;
};

function InfoRow({ icon, text }: InfoRowProps) {
  return (
    <Group gap="sm" wrap="nowrap">
      <ThemeIcon variant="light" color="gray">
        {icon}
      </ThemeIcon>
      <Text size="sm">{text}</Text>
    </Group>
  );
}

type MetricProps = {
  icon: ReactNode;
  label: string;
  value: string;
};

function Metric({ icon, label, value }: MetricProps) {
  return (
    <Paper withBorder p="sm">
      <Group gap="sm" wrap="nowrap">
        <ThemeIcon variant="light" color="gray">
          {icon}
        </ThemeIcon>
        <Box>
          <Text size="xs" c="dimmed">
            {label}
          </Text>
          <Text fw={650}>{value}</Text>
        </Box>
      </Group>
    </Paper>
  );
}

export default App;

type PublicBookingPageProps = {
  profile: BookingProfile;
  meeting: Meeting;
  slots: TimeSlot[];
  selectedSlot: TimeSlot | null;
  selectedDate: string;
  guest: GuestForm;
  publicBooked: { start: string; end: string } | null;
  publicError: string | null;
  isSubmitting: boolean;
  dateOptions: string[];
  onDateSelect: (date: string) => void;
  onSlotSelect: (slot: TimeSlot) => void;
  onGuestChange: (guest: GuestForm) => void;
  onSubmit: () => void;
  getSlotsByDate: () => Map<string, TimeSlot[]>;
};

function PublicBookingPage({
  profile,
  meeting,
  slots: _slots,
  selectedSlot,
  selectedDate,
  guest,
  publicBooked,
  publicError,
  isSubmitting,
  dateOptions: _dateOptions,
  onDateSelect,
  onSlotSelect,
  onGuestChange,
  onSubmit,
  getSlotsByDate,
}: PublicBookingPageProps) {
  const slotsByDate = getSlotsByDate();
  const availableDates = Array.from(slotsByDate.keys()).sort();
  const todaySlots = slotsByDate.get(selectedDate) ?? [];

  return (
    <Container size="md" py="xl">
      <Stack gap="lg">
        <Card withBorder p="lg">
          <Group gap="md" wrap="nowrap">
            <Avatar size={56} src={profile.avatarUrl} radius="xl" color="dark">
              {initials(profile.displayName)}
            </Avatar>
            <Box>
              <Text fw={700} size="lg">{meeting.title}</Text>
              <Group gap="xs" mt={4}>
                <Badge variant="light" color="dark">{meeting.durationMinutes} min</Badge>
                <Badge variant="outline" color="gray">{meeting.timezone}</Badge>
              </Group>
            </Box>
          </Group>
          {meeting.description && (
            <Text size="sm" c="dimmed" mt="md">{meeting.description}</Text>
          )}
          <Group mt="md" gap="sm">
            <Text size="sm" c="dimmed">by {profile.displayName}</Text>
            {profile.bio && <Text size="sm" c="dimmed">— {profile.bio}</Text>}
          </Group>
        </Card>

        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="lg">
          <Card withBorder p="md">
            <Stack>
              <Title order={3}>Select date & time</Title>
              {availableDates.length === 0 ? (
                <Text c="dimmed" size="sm" py="md">No available slots</Text>
              ) : (
                <>
                  <Group gap="xs" wrap="nowrap">
                    {availableDates.map((dateValue) => (
                      <Button
                        key={dateValue}
                        size="sm"
                        variant={dateValue === selectedDate ? "filled" : "default"}
                        color="dark"
                        onClick={() => onDateSelect(dateValue)}
                      >
                        {formatDayLabel(dateValue)}
                      </Button>
                    ))}
                  </Group>

                  {todaySlots.length === 0 ? (
                    <Text c="dimmed" size="sm" py="md">No slots for this date</Text>
                  ) : (
                    <ScrollArea h={250} offsetScrollbars>
                      <Stack gap={4}>
                        {todaySlots.map((slot) => (
                          <Paper
                            key={slot.start}
                            withBorder
                            p="sm"
                            style={{
                              cursor: publicBooked ? "default" : "pointer",
                              backgroundColor: selectedSlot?.start === slot.start ? "var(--mantine-color-dark-1)" : undefined,
                              opacity: publicBooked ? 0.5 : 1,
                            }}
                            onClick={() => { if (!publicBooked) onSlotSelect(slot); }}
                          >
                            <Group justify="space-between">
                              <Text size="sm" fw={selectedSlot?.start === slot.start ? 700 : 400}>
                                {formatTime(slot.start, guestTimezone)} — {formatTime(slot.end, guestTimezone)}
                              </Text>
                              <Badge variant="light" size="sm" color={selectedSlot?.start === slot.start ? "white" : "gray"}>
                                {meeting.durationMinutes}m
                              </Badge>
                            </Group>
                          </Paper>
                        ))}
                      </Stack>
                    </ScrollArea>
                  )}
                </>
              )}
            </Stack>
          </Card>

          <Card withBorder p="md">
            <Stack>
              <Title order={3}>Your details</Title>
              {publicBooked ? (
                <Paper withBorder p="sm" bg="green.0">
                  <Group gap="sm" wrap="nowrap">
                    <ThemeIcon color="green" variant="light"><Check size={16} /></ThemeIcon>
                    <Box>
                      <Text size="sm" fw={600}>Booking confirmed</Text>
                      <Text size="xs" c="dimmed">
                        {formatLongDate(publicBooked.start)} · {formatTime(publicBooked.start, guestTimezone)}
                      </Text>
                    </Box>
                  </Group>
                </Paper>
              ) : publicError ? (
                <Paper withBorder p="sm" bg="red.0">
                  <Text size="sm" c="red" fw={500}>{publicError}</Text>
                </Paper>
              ) : (
                <>
                  <TextInput label="Name" value={guest.name} onChange={(e) => onGuestChange({ ...guest, name: e.currentTarget.value })} />
                  <TextInput label="Email" type="email" value={guest.email} onChange={(e) => onGuestChange({ ...guest, email: e.currentTarget.value })} />
                  <Textarea label="Notes" minRows={3} value={guest.notes} onChange={(e) => onGuestChange({ ...guest, notes: e.currentTarget.value })} />
                  <Button
                    color="dark"
                    leftSection={<CalendarDays size={16} />}
                    loading={isSubmitting}
                    disabled={!selectedSlot || !guest.name || !guest.email}
                    onClick={onSubmit}
                  >
                    Confirm
                  </Button>
                </>
              )}
            </Stack>
          </Card>
        </SimpleGrid>
      </Stack>
    </Container>
  );
}
