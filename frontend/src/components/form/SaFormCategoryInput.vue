<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <SaCategoryInput
      v-model="inputValue"
      :placeholder="props.placeholder"
      :clearable="props.clearable"
      :category-type="props.categoryType"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import SaCategoryInput from '@/components/category/SaCategoryInput.vue';
  import type { CategoryType } from '@/services/api/gql/graphql';

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
    clearable?: boolean,
    categoryType?: CategoryType,
  }>();

  const inputValue = ref<string | undefined>(undefined);
</script>
