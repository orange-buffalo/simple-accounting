<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <SaCustomerInput
      v-model="inputValue"
      :placeholder="props.placeholder"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import SaCustomerInput from '@/components/customer/SaCustomerInput.vue';

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
  }>();

  const inputValue = ref<number | undefined>(undefined);
</script>
