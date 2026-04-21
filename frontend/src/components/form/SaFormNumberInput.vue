<template>
  <SaFormItemInternal v-bind="props" v-model="formValue">
    <ElInput
      v-model="displayValue"
      :placeholder="placeholder"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
  }>();

  const displayValue = ref<string>('');
  const formValue = ref<number | null>(null);

  watch(() => formValue.value, (value) => {
    const newDisplay = value !== null && value !== undefined ? String(value) : '';
    if (displayValue.value !== newDisplay) {
      displayValue.value = newDisplay;
    }
  }, { immediate: true });

  watch(() => displayValue.value, (value) => {
    if (value === '' || value === null || value === undefined) {
      formValue.value = null;
    } else {
      const num = Number(value);
      formValue.value = isNaN(num) ? null : num;
    }
  });
</script>
