<template>
  <ElFormItem
    :prop="prop"
    :label="props.label"
    ref="formItemContext"
  >
    <SaInputLoader :loading="saFormApi.loading.value">
      <slot />
    </SaInputLoader>
  </ElFormItem>
</template>

<script lang="ts" setup>
/**
 * This is and internal component for form implementation. All form components should be wrapped in this component
 * in their implementation.
 */
import { ElFormItem, type FormItemContext } from 'element-plus';
import { onMounted, onUnmounted, ref, watch } from 'vue';
import type { SaFormComponentProps } from '@/components/form/sa-form-api.ts';
import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api.ts';
import SaInputLoader from '@/components/SaInputLoader.vue';
import { ensureDefined } from '@/services/utils.ts';

const props = defineProps<SaFormComponentProps>();

const formItemContext = ref<FormItemContext | null>(null);
const saFormApi = useSaFormComponentsApi();

const formItemValue = defineModel<unknown | null>();

// changes made by the wrapped components must be reflected in the form values
watch(
  () => formItemValue.value,
  (value) => {
    formItemContext.value?.clearValidate();

    const path = props.prop.split('.');
    let current = saFormApi.formValues.value;
    for (let i = 0; i < path.length - 1; i += 1) {
      if (!current[path[i]]) {
        current[path[i]] = {};
      }
      current = current[path[i]] as Record<string, unknown>;
    }
    current[path[path.length - 1]] = value;
  },
);

// changes made to the form values must be reflected in the wrapped components
watch(
  () => saFormApi.formValues.value,
  (value) => {
    const path = props.prop.split('.');
    let current = value;
    for (let i = 0; i < path.length; i += 1) {
      if (!current) {
        break;
      }
      current = current[path[i]] as Record<string, unknown>;
    }
    formItemValue.value = current;
  },
  {
    immediate: true,
    deep: true,
  },
);

onMounted(() => {
  saFormApi.registerFormItem(props.prop, ensureDefined(formItemContext.value));
});

onUnmounted(() => {
  saFormApi.unregisterFormItem(props.prop);
});
</script>
