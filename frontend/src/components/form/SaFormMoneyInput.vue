<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <SaMoneyInput
      v-model="inputValue"
      :currency="currency"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import SaMoneyInput from '@/components/SaMoneyInput.vue';

  const props = defineProps<SaFormComponentProps & {
    currency: string,
  }>();

  const inputValue = ref<number | undefined>(undefined);
</script>
