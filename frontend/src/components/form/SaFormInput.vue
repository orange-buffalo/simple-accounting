<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <ElInput
      v-model="inputValue"
      :type="type"
      :placeholder="placeholder"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';

  const inputValue = ref<string | null>();

  const props = defineProps<SaFormComponentProps & {
    type?: string,
    placeholder?: string,
  }>();
</script>
