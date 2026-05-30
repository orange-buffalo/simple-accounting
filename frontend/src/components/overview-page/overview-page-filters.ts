export type SaOverviewFilters = object;

type FilterValue = string | number | boolean;
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

export type SaOverviewFilterConfig<TValue> = SaOverviewMultiSelectFilterConfig<TValue>
  | SaOverviewTextFilterConfig<TValue>;

export type SaOverviewFilterConfigs<TFilters extends SaOverviewFilters> = {
  [TKey in keyof TFilters]-?: SaOverviewFilterConfig<TFilters[TKey]>
};

export function createOverviewFilters<TFilters extends SaOverviewFilters>(
  filters?: TFilters,
): TFilters {
  return { ...filters } as TFilters;
}
