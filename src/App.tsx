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
  NavLink,
  Paper,
  ScrollArea,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
  ThemeIcon,
  Title,
  Tooltip,
} from "@mantine/core";
import {
  CalendarDays,
  Check,
  Clock3,
  Copy,
  Link,
  Settings2,
  RotateCcw,
  UserRound,
  Video,
  CalendarClock,
} from "lucide-react";
import {
  api,
  type AvailabilitySchedule,
  type AvailabilityRule,
  type Booking,
  type BookingInfo,
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
  return date.toISOString().slice(0, 10);
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

function cloneSchedule(schedule: AvailabilitySchedule) {
  return {
    ...schedule,
    rules: schedule.rules.map((rule) => ({ ...rule })),
    dateOverrides: schedule.dateOverrides.map((override) => ({ ...override })),
  };
}

function App() {
  const [view, setView] = useState<ViewMode>("booking");
  const [bookingInfo, setBookingInfo] = useState<BookingInfo | null>(null);
  const [slots, setSlots] = useState<TimeSlot[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [schedules, setSchedules] = useState<AvailabilitySchedule[]>([]);
  const [availabilityDraft, setAvailabilityDraft] = useState<AvailabilitySchedule | null>(null);
  const [selectedDate, setSelectedDate] = useState(toDateInputValue(addDays(new Date(), 1)));
  const [selectedSlot, setSelectedSlot] = useState<TimeSlot | null>(null);
  const [guest, setGuest] = useState<GuestForm>({ name: "", email: "", notes: "" });
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmation, setConfirmation] = useState<Booking | null>(null);

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

        const [bookingResponse, scheduleResponse] = await Promise.all([
          api.listBookings(info.profile.id),
          api.listAvailabilitySchedules(info.profile.id),
        ]);

        if (!isMounted) return;
        setBookings(bookingResponse.items);
        setSchedules(scheduleResponse.items);
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
    const activeSchedule = schedules[0];
    setAvailabilityDraft(activeSchedule ? cloneSchedule(activeSchedule) : null);
  }, [schedules]);

  useEffect(() => {
    let isMounted = true;

    async function loadSlots() {
      if (!bookingInfo) return;
      const response = await api.listSlots(
        bookingInfo.profile.username,
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
  }, [bookingInfo, selectedDate]);

  async function submitBooking() {
    if (!bookingInfo || !selectedSlot || !guest.name || !guest.email) return;
    setIsSubmitting(true);
    try {
      const booking = await api.createBooking({
        username: bookingInfo.profile.username,
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
                {bookingInfo.onlineCall.durationMinutes} min · {bookingInfo.onlineCall.timezone}
              </Text>
            </Box>
          </Group>

          {view === "booking" ? (
            <BookingView
              bookingInfo={bookingInfo}
              dateOptions={dateOptions}
              selectedDate={selectedDate}
              selectedSlot={selectedSlot}
              slots={slots}
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
              onGuestChange={setGuest}
              onSubmit={submitBooking}
            />
          ) : view === "workspace" ? (
            <WorkspaceView bookingInfo={bookingInfo} bookings={bookings} />
          ) : view === "availability" ? (
            <AvailabilityView
              activeSchedule={availabilityDraft}
              onRuleChange={(weekday, field, value) => {
                setAvailabilityDraft((current) =>
                  current
                    ? {
                        ...current,
                        rules: current.rules.map((rule) =>
                          rule.weekday === weekday ? { ...rule, [field]: value } : rule,
                        ),
                      }
                    : current,
                );
              }}
              onReset={() => {
                const activeSchedule = schedules[0];
                setAvailabilityDraft(activeSchedule ? cloneSchedule(activeSchedule) : null);
              }}
            />
          ) : null}
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}

type BookingViewProps = {
  bookingInfo: BookingInfo;
  dateOptions: string[];
  selectedDate: string;
  selectedSlot: TimeSlot | null;
  slots: TimeSlot[];
  guest: GuestForm;
  confirmation: Booking | null;
  isSubmitting: boolean;
  onDateChange: (value: string) => void;
  onSlotChange: (slot: TimeSlot) => void;
  onGuestChange: (guest: GuestForm) => void;
  onSubmit: () => void;
};

function BookingView({
  bookingInfo,
  dateOptions,
  selectedDate,
  selectedSlot,
  slots,
  guest,
  confirmation,
  isSubmitting,
  onDateChange,
  onSlotChange,
  onGuestChange,
  onSubmit,
}: BookingViewProps) {
  const canSubmit = Boolean(selectedSlot && guest.name && guest.email);

  return (
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
            <InfoRow icon={<Clock3 size={16} />} text={`${bookingInfo.onlineCall.durationMinutes} minutes`} />
            <InfoRow icon={<Video size={16} />} text="Online meeting" />
            <InfoRow icon={<UserRound size={16} />} text={guestTimezone} />
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
                variant={dateValue === selectedDate ? "filled" : "default"}
                color="dark"
                onClick={() => onDateChange(dateValue)}
              >
                {new Date(`${dateValue}T12:00:00`).getDate()}
              </Button>
            ))}
          </SimpleGrid>

          <ScrollArea h={330} offsetScrollbars>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xs">
              {slots.map((slot) => (
                <Button
                  key={slot.start}
                  variant={selectedSlot?.start === slot.start ? "filled" : "default"}
                  color="dark"
                  onClick={() => onSlotChange(slot)}
                >
                  {formatTime(slot.start, guestTimezone)}
                </Button>
              ))}
            </SimpleGrid>
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
  );
}

type WorkspaceViewProps = {
  bookingInfo: BookingInfo;
  bookings: Booking[];
};

function WorkspaceView({ bookingInfo, bookings }: WorkspaceViewProps) {
  const bookingUrl = `${window.location.origin}/?username=${bookingInfo.profile.username}`;

  return (
    <Stack>
      <Card withBorder>
        <Stack>
          <Group justify="space-between" align="flex-start">
            <Box>
              <Title order={2}>{bookingInfo.onlineCall.title}</Title>
              <Text size="sm" c="dimmed">
                {bookingInfo.onlineCall.description}
              </Text>
            </Box>
            <Badge color={bookingInfo.onlineCall.isActive ? "green" : "gray"} variant="light">
              {bookingInfo.onlineCall.isActive ? "Active" : "Paused"}
            </Badge>
          </Group>

          <SimpleGrid cols={{ base: 1, sm: 2 }}>
            <Metric icon={<Clock3 size={16} />} label="Duration" value={`${bookingInfo.onlineCall.durationMinutes}m`} />
            <Metric icon={<CalendarDays size={16} />} label="Notice" value={`${bookingInfo.onlineCall.minimumNoticeMinutes}m`} />
            <Metric icon={<Settings2 size={16} />} label="Interval" value={`${bookingInfo.onlineCall.slotIntervalMinutes}m`} />
            <Metric icon={<Video size={16} />} label="Buffer" value={`${bookingInfo.onlineCall.bufferBeforeMinutes}/${bookingInfo.onlineCall.bufferAfterMinutes}m`} />
          </SimpleGrid>

          <Group justify="space-between" gap="sm" wrap="nowrap">
            <Group gap="sm" wrap="nowrap">
              <ThemeIcon variant="light" color="gray">
                <Link size={16} />
              </ThemeIcon>
              <Text size="sm" truncate="end">
                {bookingUrl}
              </Text>
            </Group>
            <Tooltip label="Copy link">
              <ActionIcon variant="default" aria-label="Copy link">
                <Copy size={16} />
              </ActionIcon>
            </Tooltip>
          </Group>
        </Stack>
      </Card>

      <BookingsView bookings={bookings} />
    </Stack>
  );
}

type AvailabilityViewProps = {
  activeSchedule?: AvailabilitySchedule | null;
  onRuleChange: (
    weekday: AvailabilityRule["weekday"],
    field: "startTime" | "endTime",
    value: string,
  ) => void;
  onReset: () => void;
};

function AvailabilityView({ activeSchedule, onRuleChange, onReset }: AvailabilityViewProps) {
  return (
    <Card withBorder>
      <Stack>
        <Group justify="space-between" align="flex-start">
          <Box>
            <Title order={2}>Availability</Title>
            <Text size="sm" c="dimmed">
              {activeSchedule?.timezone}
            </Text>
          </Box>
          <Button variant="default" leftSection={<RotateCcw size={16} />} onClick={onReset}>
            Reset
          </Button>
        </Group>

        <Stack gap="xs">
          {weekdayOrder.map((weekday) => {
            const rule = activeSchedule?.rules.find((entry) => entry.weekday === weekday);

            if (!rule) return null;

            return (
              <Group key={weekday} align="flex-end" wrap="nowrap">
                <Text size="sm" w={110} tt="capitalize">
                  {weekdayLabels[weekday]}
                </Text>
                <TextInput
                  label="Start"
                  type="time"
                  value={rule.startTime}
                  onChange={(event) => onRuleChange(weekday, "startTime", event.currentTarget.value)}
                />
                <TextInput
                  label="End"
                  type="time"
                  value={rule.endTime}
                  onChange={(event) => onRuleChange(weekday, "endTime", event.currentTarget.value)}
                />
              </Group>
            );
          })}
        </Stack>
      </Stack>
    </Card>
  );
}

type BookingsViewProps = {
  bookings: Booking[];
};

function BookingsView({ bookings }: BookingsViewProps) {
  return (
    <Card withBorder>
      <Group justify="space-between" mb="md">
        <Title order={2}>Bookings</Title>
        <Badge variant="outline" color="gray">
          {bookings.length}
        </Badge>
      </Group>
      <Table.ScrollContainer minWidth={640}>
        <Table verticalSpacing="md" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Guest</Table.Th>
              <Table.Th>Time</Table.Th>
              <Table.Th>Status</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {bookings.map((booking) => (
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
