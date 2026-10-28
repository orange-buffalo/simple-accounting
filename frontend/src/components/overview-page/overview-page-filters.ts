export type SaOverviewFilters = object;

type FilterValue = string | number | boolean;
export const overviewFilterValueKey = (value: FilterValue): string => `${typeof value}:${String(value)}`;
type ArrayFilterValue = FilterValue[] | readonly FilterValue[] | null | undefined;
type ArrayFilterItem<TValue> = NonNullable<TValue> extends readonly (infer TItem)[] ? TItem : never;

export type SaOverviewFilterOption<TValue extends FilterValue> = {
  label: string,
  value: TValue,
};

export type SaOverviewMultiSelectFilterConfig<TValue> = TValue extends ArrayFilterValue
  ? {
    type: 'multi-select',
    label: string,
    options: SaOverviewFilterOption<ArrayFilterItem<TValue> & FilterValue>[],
  }
  : never;

export type SaOverviewTextFilterConfig<TValue> = TValue extends string | null | undefined
  ? {
    type: 'text',
    label: string,
  }
  : never;

export type SaOverviewDateRangeFilterConfig<TValue> = TValue extends [string, string] | null
  ? {
    type: 'date-range',
    label: string,
  }
  : never;

export type SaOverviewFilterConfig<TValue> = SaOverviewMultiSelectFilterConfig<TValue>
  | SaOverviewTextFilterConfig<TValue>
  | SaOverviewDateRangeFilterConfig<TValue>;

export type SaOverviewFilterConfigs<TFilters extends SaOverviewFilters> = {
  [TKey in keyof TFilters]-?: SaOverviewFilterConfig<TFilters[TKey]>
};

export function createOverviewFilters<TFilters extends SaOverviewFilters>(
  filters?: TFilters,
): TFilters {
  return { ...filters } as TFilters;
}

export function updateOverviewFilterValue<TFilters extends SaOverviewFilters>(
  filters: TFilters | undefined,
  key: keyof TFilters,
  value: unknown,
): TFilters {
  return { ...filters, [key]: value } as TFilters;
}

export function toggleOverviewFilterValue(
  current: unknown,
  value: FilterValue,
  selected: boolean,
): FilterValue[] | null {
  const values: FilterValue[] = Array.isArray(current) ? current.filter((item) => item !== value) : [];
  if (selected) values.push(value);
  return values.length ? values : null;
}

export function clearOverviewFilterValues<TFilters extends SaOverviewFilters>(
  filters: TFilters | undefined,
  configs: Partial<SaOverviewFilterConfigs<TFilters>>,
): TFilters {
  return { ...filters, ...Object.fromEntries(Object.keys(configs).map((key) => [key, null])) } as TFilters;
}
