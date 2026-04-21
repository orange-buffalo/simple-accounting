<template>
  <SaFormItemInternal v-bind="props" :label="undefined" v-model="checkboxValue">
    <ElCheckbox v-model="checkboxValue">{{ props.label }}</ElCheckbox>
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';

  const props = defineProps<SaFormComponentProps>();

  const checkboxValue = ref<boolean | null>();
</script>
