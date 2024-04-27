<template>
  <ElFormItem
    :prop="prop"
    :label="label"
    ref="formItemContext"
  >
    <slot />
  </ElFormItem>
</template>

<script lang="ts" setup>
  import { ElFormItem, FormItemContext } from 'element-plus';
  import {
    defineProps, onMounted, onUnmounted, ref,
  } from 'vue';
  import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api.ts';
  import { ensureDefined } from '@/services/utils.ts';

  const props = defineProps<{
    prop: string,
    label: string,
  }>();

  const formItemContext = ref<FormItemContext | null>(null);
  const saFormApi = useSaFormComponentsApi();

  onMounted(() => {
    saFormApi.registerFormItem(props.prop, ensureDefined(formItemContext.value));
  });

  onUnmounted(() => {
    saFormApi.unregisterFormItem(props.prop);
  });
</script>
