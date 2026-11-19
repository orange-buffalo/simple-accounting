import { afterEach, describe, expect, it, vi } from 'vitest';
import { effectScope } from 'vue';
import {
  calendarDayStart, currentCalendarDate, currentDate, getCurrentTimeZone,
  setProfileTimeZone, setSharedWorkspaceTimeZone,
  calendarDateTimeToInstant, timestampToCalendarDateTime, useProfileTimeZone,
} from '@/services/time-zone';

afterEach(() => {
  setProfileTimeZone(null);
  setSharedWorkspaceTimeZone(false);
  vi.useRealTimers();
  vi.unstubAllEnvs();
});

describe('preferred time zone', () => {
  it('defaults calendar controls to the profile month rather than the browser month', () => {
    vi.stubEnv('TZ', 'America/New_York');
    vi.useFakeTimers();
    vi.setSystemTime(new Date('1999-03-31T23:01:02Z'));
    setProfileTimeZone('Australia/Melbourne');
    expect(new Date().getMonth()).toBe(2);
    expect(currentCalendarDate()).toEqual(new Date(1999, 3, 1));
    setSharedWorkspaceTimeZone(true);
    expect(currentCalendarDate()).toEqual(new Date(1999, 2, 31));
  });

  it('preserves a profile wall time inside a browser DST gap', () => {
    vi.stubEnv('TZ', 'America/New_York');
    setProfileTimeZone('Europe/Paris');
    const wallTime = timestampToCalendarDateTime(new Date('1999-04-04T00:30:00Z'));
    expect(wallTime).toBe('1999-04-04T02:30:00.000');
    expect(calendarDateTimeToInstant(wallTime)).toBe('1999-04-04T00:30:00.000Z');
  });

  it('uses the profile for owned-workspace management without changing the selected shared workspace', () => {
    vi.stubEnv('TZ', 'America/New_York');
    setProfileTimeZone('Australia/Melbourne');
    setSharedWorkspaceTimeZone(true);
    const scope = effectScope();
    try {
      scope.run(() => useProfileTimeZone());
      expect(getCurrentTimeZone()).toBe('Australia/Melbourne');
    } finally {
      scope.stop();
    }
    expect(getCurrentTimeZone()).toBe('America/New_York');
  });
  it.each([
    ['Australia/Melbourne', '1999-03-29'],
    ['America/New_York', '1999-03-28'],
  ])('uses %s for the current calendar date', (zone, date) => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('1999-03-28T23:01:02Z'));
    setProfileTimeZone(zone);
    expect(currentDate()).toBe(date);
  });

  it('uses the browser zone for shared workspaces and restores the profile for owned workspaces', () => {
    vi.stubEnv('TZ', 'America/New_York');
    vi.useFakeTimers();
    vi.setSystemTime(new Date('1999-03-28T23:01:02Z'));
    setProfileTimeZone('Australia/Melbourne');
    setSharedWorkspaceTimeZone(true);
    expect(getCurrentTimeZone()).toBe('America/New_York');
    expect(currentDate()).toBe('1999-03-28');
    setSharedWorkspaceTimeZone(false);
    expect(getCurrentTimeZone()).toBe('Australia/Melbourne');
    expect(currentDate()).toBe('1999-03-29');
  });

  it.each([
    ['1999-04-04', '1999-04-04T05:00:00.000Z', '1999-04-05T04:00:00.000Z'],
    ['1999-10-31', '1999-10-31T04:00:00.000Z', '1999-11-01T05:00:00.000Z'],
  ])('uses calendar midnights across DST on %s', (date, start, end) => {
    setProfileTimeZone('America/New_York');
    expect(calendarDayStart(date)).toBe(start);
    expect(calendarDayStart(date, 1)).toBe(end);
  });
});
