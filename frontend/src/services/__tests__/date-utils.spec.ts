import { describe, expect, it } from 'vitest';
import { getAustralianFinancialYearDateRange } from '@/services/date-utils';

describe('getAustralianFinancialYearDateRange', () => {
  it('returns the financial year starting in the current calendar year after June', () => {
    const range = getAustralianFinancialYearDateRange(new Date(3025, 6, 1));

    expect(range).toEqual([new Date(3025, 6, 1), new Date(3026, 5, 30)]);
  });

  it('returns the financial year starting in the previous calendar year before July', () => {
    const range = getAustralianFinancialYearDateRange(new Date(3025, 5, 30));

    expect(range).toEqual([new Date(3024, 6, 1), new Date(3025, 5, 30)]);
  });
});
