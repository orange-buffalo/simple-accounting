import { onScopeDispose, ref } from 'vue';
import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

dayjs.extend(utc);
dayjs.extend(timezone);

const profileTimeZone = ref<string | null>(null);
const sharedWorkspace = ref(false);
const profileZoneScopes = ref(0);

export function useProfileTimeZone() {
  profileZoneScopes.value += 1;
  onScopeDispose(() => { profileZoneScopes.value -= 1; });
}

export function setProfileTimeZone(timeZone: string | null) {
  profileTimeZone.value = timeZone;
}

export function setSharedWorkspaceTimeZone(shared: boolean) {
  sharedWorkspace.value = shared;
}

export function getCurrentTimeZone(): string {
  return ((!sharedWorkspace.value || profileZoneScopes.value > 0) && profileTimeZone.value)
    || Intl.DateTimeFormat().resolvedOptions().timeZone;
}

export function currentDate(): string {
  return dayjs().tz(getCurrentTimeZone()).format('YYYY-MM-DD');
}

/** Calendar-only Date for controls that use browser-local Date fields. */
export function currentCalendarDate(): Date {
  const [year, month, day] = currentDate().split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function calendarDayStart(date: string, dayOffset = 0): string {
  const day = dayjs.utc(date).add(dayOffset, 'day').format('YYYY-MM-DD');
  return dayjs.tz(day, getCurrentTimeZone()).toISOString();
}

export function timestampToCalendarDateTime(date: Date): string {
  return dayjs(date).tz(getCurrentTimeZone()).format('YYYY-MM-DDTHH:mm:ss.SSS');
}

/** UTC fields avoid browser DST normalization while formatting another zone's wall time. */
export function timestampToUtcCalendarDate(date: Date): Date {
  const zoned = dayjs(date).tz(getCurrentTimeZone());
  return new Date(Date.UTC(
    zoned.year(), zoned.month(), zoned.date(), zoned.hour(), zoned.minute(), zoned.second(), zoned.millisecond(),
  ));
}

export function calendarDateTimeToInstant(date: string): string {
  return dayjs.tz(date, getCurrentTimeZone()).toISOString();
}

export function getSupportedTimeZones(): string[] {
  const zones = [...Intl.supportedValuesOf('timeZone'), getCurrentTimeZone(), 'Australia/Melbourne', 'UTC'];
  return [...new Set(zones)].sort();
}
