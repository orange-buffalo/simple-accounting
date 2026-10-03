<template>
  <SaFormSelect v-bind="props" :disabled="!props.currency || loading || !data.length" filterable>
    <ElOption
      v-for="country in countries"
      :key="country.code"
      :value="country.code"
      :label="country.name"
    />
  </SaFormSelect>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import SaFormSelect from '@/components/form/SaFormSelect.vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api';
  import { getCountryName } from '@/services/i18n/countries';
  import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api';

  const props = defineProps<SaFormComponentProps & {
    currency?: string | null,
  }>();
  const formApi = useSaFormComponentsApi();
  const data = ref<string[]>([]);
  const loading = ref(false);
  const loadCountries = useLazyQuery(graphql(`
    query countriesForResidency($currency: String!) {
      countries(currency: $currency)
    }
  `), 'countries');
  watch(() => props.currency, async (currency, _, onCleanup) => {
    let active = true;
    onCleanup(() => { active = false; });
    data.value = [];
    const values = formApi.formValues.value as Record<string, unknown>;
    if (!currency) {
      values[props.prop] = null;
    }
    loading.value = !!currency;
    if (currency) {
      try {
        const result = await loadCountries({ currency });
        if (active) {
          data.value = result;
          const currentValues = formApi.formValues.value as Record<string, unknown>;
          if (!result.includes(currentValues[props.prop] as string)) {
            currentValues[props.prop] = null;
          }
        }
      } catch (error: unknown) {
        if (active) {
          const currentValues = formApi.formValues.value as Record<string, unknown>;
          currentValues[props.prop] = null;
          throw error;
        }
      } finally {
        if (active) loading.value = false;
      }
    }
  }, { immediate: true });
  const countries = computed(() => data.value
    .map((code) => ({ code, name: getCountryName(code) }))
    .sort((a, b) => a.name.localeCompare(b.name)));
</script>
