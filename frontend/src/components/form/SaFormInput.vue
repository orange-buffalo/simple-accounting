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
import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
import type { SaFormComponentProps } from '@/components/form/sa-form-api';

const inputValue = ref<string | null>();

const props = defineProps<
  SaFormComponentProps & {
    type?: string;
    placeholder?: string;
  }
>();
</script>
