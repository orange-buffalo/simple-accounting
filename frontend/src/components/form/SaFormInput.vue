<template>
  <ElInput
    v-model="inputValue"
  />
</template>

<script lang="ts" setup>
  import { useSaFormItemComponentsApi } from '@/components/form/sa-form-item-components-api.ts';

  const saFormItemApi = useSaFormItemComponentsApi();
  const inputValue = saFormItemApi.formItemValue;
</script>
