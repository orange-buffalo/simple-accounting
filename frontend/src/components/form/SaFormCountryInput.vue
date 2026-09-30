<template>
  <SaFormSelect v-bind="props" filterable>
    <ElOption
      v-for="country in countries"
      :key="country.code"
      :value="country.code"
      :label="country.name"
    />
  </SaFormSelect>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import SaFormSelect from '@/components/form/SaFormSelect.vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import { graphql } from '@/services/api/gql';
  import { useQuery } from '@/services/api/use-gql-api';
  import { getCountryName } from '@/services/i18n/countries';

  const props = defineProps<SaFormComponentProps>();
  const [, data] = useQuery(graphql(`
    query countriesForResidency {
      countries
    }
  `), 'countries');
  const countries = computed(() => (data.value ?? [])
    .map((code) => ({ code, name: getCountryName(code) }))
    .sort((a, b) => a.name.localeCompare(b.name)));
</script>
