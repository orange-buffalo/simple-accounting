<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <ElDatePicker
      v-model="inputValue"
      type="date"
      value-format="YYYY-MM-DD"
      :format="datePickerFormat"
      :placeholder="placeholder"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { ElDatePicker } from 'element-plus';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import { datePickerFormat } from '@/components/date-picker/date-picker-localization';

  const inputValue = ref<string | null>(null);

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
  }>();
</script>
