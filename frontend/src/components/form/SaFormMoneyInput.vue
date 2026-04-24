<template>
  <SaFormItemInternal v-bind="props" v-model="formValue">
    <SaMoneyInput
      v-model="inputValue"
      :currency="currency"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import SaMoneyInput from '@/components/SaMoneyInput.vue';

  const props = defineProps<SaFormComponentProps & {
    currency: string,
  }>();

  const formValue = ref<number>(0);
  const inputValue = ref<number | undefined>(undefined);

  watch(() => formValue.value, (value) => {
    const newInputValue = value === 0 ? undefined : value;
    if (inputValue.value !== newInputValue) {
      inputValue.value = newInputValue;
    }
  }, { immediate: true });

  watch(() => inputValue.value, (value) => {
    const newFormValue = value ?? 0;
    if (formValue.value !== newFormValue) {
      formValue.value = newFormValue;
    }
  });
</script>
