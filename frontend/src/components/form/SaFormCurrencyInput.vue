<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <SaCurrencyInput
      v-model="inputValue"
      :disabled="props.disabled"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import SaCurrencyInput from '@/components/currency-input/SaCurrencyInput.vue';

  const inputValue = ref<string | undefined>();

  const props = defineProps<SaFormComponentProps & {
    disabled?: boolean,
  }>();
</script>
