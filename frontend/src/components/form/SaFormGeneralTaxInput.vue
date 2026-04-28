<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <SaGeneralTaxInput
      v-model="inputValue"
      :placeholder="props.placeholder"
      :clearable="props.clearable"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput.vue';

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
    clearable?: boolean,
  }>();

  const inputValue = ref<number | undefined>(undefined);
</script>
