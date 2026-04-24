<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
    <ElDatePicker
      v-model="inputValue"
      type="date"
      value-format="YYYY-MM-DD"
      :placeholder="placeholder"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { ElDatePicker } from 'element-plus';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';

  const inputValue = ref<string | null>(null);

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
  }>();
</script>
