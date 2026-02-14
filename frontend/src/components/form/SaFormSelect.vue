<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <ElSelect
      v-model="inputValue"
      :placeholder="props.placeholder"
      :clearable="props.clearable"
      :disabled="props.disabled"
      :filterable="props.filterable"
      @change="onChange"
    >
      <slot />
    </ElSelect>
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
import { SaFormComponentProps } from '@/components/form/sa-form-api';
import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api';

const inputValue = ref<unknown | null>();

const props = defineProps<
  SaFormComponentProps & {
    clearable?: boolean;
    placeholder?: string;
    disabled?: boolean;
    filterable?: boolean;
    submitOnChange?: boolean;
  }
>();

const formApi = useSaFormComponentsApi();

const onChange = async () => {
  if (props.submitOnChange) {
    await formApi.submitForm();
  }
};
</script>
