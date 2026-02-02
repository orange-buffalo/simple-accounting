/**
 * Formats a Date object to ISO date string (YYYY-MM-DD) using the browser's local timezone.
 * 
 * This function avoids the timezone conversion issue with toISOString() which converts to UTC.
 * Instead, it uses the Intl.DateTimeFormat API to format the date in the local timezone.
 * 
 * For example, if a user in Australia/Melbourne (UTC+11) has a date:
 * - new Date("2023-12-15") creates a date at midnight local time
 * - toISOString() would convert to UTC, giving "2023-12-14T13:00:00.000Z" 
 * - substring(0,10) would extract "2023-12-14" ❌ Wrong!
 * 
 * This function formats using local timezone, preserving the user's intended date.
 * 
 * @param date The date to format
 * @returns ISO formatted date string (YYYY-MM-DD) in local timezone
 */
export function formatDateToLocalISOString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
