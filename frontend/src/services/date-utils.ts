/** Formats a local calendar date without converting it to UTC. */
export function formatDateToLocalISOString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function getAustralianFinancialYearDateRange(date = new Date()): [Date, Date] {
  const startYear = date.getMonth() >= 6 ? date.getFullYear() : date.getFullYear() - 1;
  return [new Date(startYear, 6, 1), new Date(startYear + 1, 5, 30)];
}
