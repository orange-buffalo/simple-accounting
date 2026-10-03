<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <ElSelect
      v-model="inputValue"
      :disabled="!props.currency || loading || !data.length"
      :loading="loading"
      :aria-busy="loading"
      :placeholder="placeholder"
      filterable
    >
      <ElOption
        v-for="country in countries"
        :key="country.code"
        :value="country.code"
        :label="country.name"
      />
    </ElSelect>
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api';
  import { getCountryName } from '@/services/i18n/countries';
  import { $t } from '@/services/i18n';

  const props = defineProps<SaFormComponentProps & {
    currency?: string | null,
  }>();
  const inputValue = ref<string | null>();
  const data = ref<string[]>([]);
  const loading = ref(false);
  const loadFailed = ref(false);
  const placeholder = computed(() => {
    if (!props.currency) return $t.value.saFormCountryInput.selectCurrency();
    if (loading.value) return $t.value.saFormCountryInput.loading();
    if (loadFailed.value) return $t.value.saFormCountryInput.loadFailed();
    if (!data.value.length) return $t.value.saFormCountryInput.noCountries();
    return undefined;
  });
  const loadCountries = useLazyQuery(graphql(`
    query countriesForResidency($currency: String!) {
      countries(currency: $currency)
    }
  `), 'countries');
  watch(() => props.currency, async (currency, previousCurrency, onCleanup) => {
    let active = true;
    onCleanup(() => { active = false; });
    data.value = [];
    loadFailed.value = false;
    if (!currency || previousCurrency) {
      inputValue.value = null;
    }
    loading.value = !!currency;
    if (currency) {
      let loaded = false;
      try {
        const result = await loadCountries({ currency });
        loaded = true;
        if (active) {
          data.value = result;
          if (inputValue.value && !result.includes(inputValue.value)) {
            inputValue.value = null;
          }
        }
      } finally {
        if (active) {
          loadFailed.value = !loaded;
          loading.value = false;
        }
      }
    }
  }, { immediate: true });
  const countries = computed(() => data.value
    .map((code) => ({ code, name: getCountryName(code) }))
    .sort((a, b) => a.name.localeCompare(b.name)));
</script>
