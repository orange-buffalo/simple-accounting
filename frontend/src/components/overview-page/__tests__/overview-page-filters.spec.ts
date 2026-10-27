import { describe, expect, it } from 'vitest';
import {
  clearOverviewFilterValues, overviewFilterValueKey, toggleOverviewFilterValue, updateOverviewFilterValue,
} from '../overview-page-filters';

describe('overview filter model', () => {
  it('keeps rendered option and selected-value identities distinct across scalar types', () => {
    expect([0, '0', false, 'false'].map(overviewFilterValueKey)).toEqual([
      'number:0', 'string:0', 'boolean:false', 'string:false',
    ]);
    const remaining = toggleOverviewFilterValue([0, '0', false, 'false'], false, false) ?? [];
    expect(remaining.map(overviewFilterValueKey)).toEqual([
      'number:0', 'string:0', 'string:false',
    ]);
  });

  it('preserves scalar option types and distinguishes zero, false and their string forms', () => {
    const values = [0, false, '0', 'false'];
    expect(toggleOverviewFilterValue(values, 0, false)).toEqual([false, '0', 'false']);
    expect(toggleOverviewFilterValue(values, false, false)).toEqual([0, '0', 'false']);
    expect(toggleOverviewFilterValue(null, false, true)).toEqual([false]);
    expect(toggleOverviewFilterValue(null, 0, true)).toEqual([0]);
    expect(toggleOverviewFilterValue([false], false, false)).toBeNull();
    expect(values).toEqual([0, false, '0', 'false']);
  });

  it('preserves unrelated model fields when updating and removing selected values', () => {
    const initial = { search: 'Slurm', selected: [0, false], workspace: 'Planet Express' };
    const updated = updateOverviewFilterValue(initial, 'search', 'Robot oil');
    expect(updated).toEqual({ search: 'Robot oil', selected: [0, false], workspace: 'Planet Express' });
    const remaining = toggleOverviewFilterValue(updated.selected, 0, false);
    const removed = updateOverviewFilterValue(updated, 'selected', remaining);
    expect(removed).toEqual({ search: 'Robot oil', selected: [false], workspace: 'Planet Express' });
    expect(initial).toEqual({ search: 'Slurm', selected: [0, false], workspace: 'Planet Express' });
  });

  it('clears only configured fields', () => {
    const initial = { search: 'Slurm', workspace: 'Planet Express' };
    expect(clearOverviewFilterValues(initial, {
      search: { type: 'text', label: 'Search' },
    })).toEqual({ search: null, workspace: 'Planet Express' });
    expect(initial).toEqual({ search: 'Slurm', workspace: 'Planet Express' });
  });
});
