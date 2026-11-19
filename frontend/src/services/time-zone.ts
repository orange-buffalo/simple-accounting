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
  return timestampToCalendarDateTime(new Date()).slice(0, 10);
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
  return timestampToUtcCalendarDate(date).toISOString().slice(0, -1);
}

/** UTC fields avoid browser DST normalization while formatting another zone's wall time. */
export function timestampToUtcCalendarDate(date: Date): Date {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: getCurrentTimeZone(),
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const value = (type: Intl.DateTimeFormatPartTypes) => Number(parts.find((part) => part.type === type)?.value);
  return new Date(Date.UTC(
    value('year'), value('month') - 1, value('day'),
    value('hour'), value('minute'), value('second'), date.getUTCMilliseconds(),
  ));
}

export function calendarDateTimeToInstant(date: string): string {
  return dayjs.tz(date, getCurrentTimeZone()).toISOString();
}

export function getSupportedTimeZones(): string[] {
  const zones = [...Intl.supportedValuesOf('timeZone'), getCurrentTimeZone(), 'UTC'];
  return [...new Set(zones)].sort();
}
