<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <SaInvoiceSelect v-model="invoiceSelectValue" />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import SaInvoiceSelect from '@/components/entity-select/SaInvoiceSelect.vue';

  const props = defineProps<SaFormComponentProps>();

  const inputValue = ref<number | null | undefined>(undefined);
  const invoiceSelectValue = computed<number | undefined>({
    get: () => inputValue.value ?? undefined,
    set: value => {
      inputValue.value = value ?? null;
    },
  });
</script>
