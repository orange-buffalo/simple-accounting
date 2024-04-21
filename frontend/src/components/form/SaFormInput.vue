<template>
  <ElInput
    v-model="inputValue"
    :type="type"
    :placeholder="placeholder"
  />
</template>

<script lang="ts" setup>
  import { useSaFormItemComponentsApi } from '@/components/form/sa-form-item-components-api.ts';

  const saFormItemApi = useSaFormItemComponentsApi();
  const inputValue = saFormItemApi.formItemValue;

  defineProps<{
    type?: string,
    placeholder?: string,
  }>();
</script>
