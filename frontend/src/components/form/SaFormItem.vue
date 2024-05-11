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
    defineProps, onMounted, onUnmounted, ref, watch,
  } from 'vue';
  import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api.ts';
  import { ensureDefined } from '@/services/utils.ts';
  import {
    provideSaFormItemComponentsApi,
    SaFormItemComponentsApi,
  } from '@/components/form/sa-form-item-components-api.ts';

  const props = defineProps<{
    prop: string,
    label: string,
  }>();

  const formItemContext = ref<FormItemContext | null>(null);
  const saFormApi = useSaFormComponentsApi();

  const formItemValue = ref<unknown | null>(null);
  const componentsApi: SaFormItemComponentsApi = {
    formItemValue,
  };
  provideSaFormItemComponentsApi(componentsApi);

  watch(() => formItemValue.value, (value) => {
    formItemContext.value?.clearValidate();

    // set the deep value by prop into saFormApi.formValues, ensure nested objects are traversed
    const path = props.prop.split('.');
    let current = saFormApi.formValues;
    for (let i = 0; i < path.length - 1; i += 1) {
      if (!current[path[i]]) {
        current[path[i]] = {};
      }
      current = current[path[i]] as Record<string, unknown>;
    }
    current[path[path.length - 1]] = value;
  });

  // update formItemValue when saFormApi.formValues[prop] changes, ensure nested objects are traversed
  watch(() => saFormApi.formValues, (value) => {
    formItemValue.value = undefined;
    const path = props.prop.split('.');
    let current = value;
    for (let i = 0; i < path.length; i += 1) {
      if (!current) {
        break;
      }
      current = current[path[i]] as Record<string, unknown>;
    }
    formItemValue.value = current;
  }, {
    immediate: true,
    deep: true,
  });

  onMounted(() => {
    saFormApi.registerFormItem(props.prop, ensureDefined(formItemContext.value));
  });

  onUnmounted(() => {
    saFormApi.unregisterFormItem(props.prop);
  });
</script>
