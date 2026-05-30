export type SaOverviewFilters = {
  freeSearchText?: string | null,
};

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

export type SaOverviewFilterConfig<TValue> = SaOverviewMultiSelectFilterConfig<TValue>;

export type SaOverviewFilterConfigs<TFilters extends SaOverviewFilters> = {
  [TKey in Exclude<keyof TFilters, 'freeSearchText'>]-?: SaOverviewFilterConfig<TFilters[TKey]>
};

export function createOverviewFilters<TFilters extends SaOverviewFilters>(
  filters?: Omit<TFilters, 'freeSearchText'>,
): TFilters {
  return {
    freeSearchText: null,
    ...filters,
  } as TFilters;
}
