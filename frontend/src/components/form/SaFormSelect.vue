<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <ElSelect
      v-model="inputValue"
      :placeholder="props.placeholder"
      :clearable="props.clearable"
      :disabled="props.disabled"
    >
      <slot />
    </ElSelect>
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';

  const inputValue = ref<unknown | null>();

  const props = defineProps<SaFormComponentProps & {
    clearable?: boolean,
    placeholder?: string,
    disabled?: boolean,
  }>();
</script>
