<template>
  <SaInputLoader :loading="loading">
    <ElSelect
      :model-value="modelValue"
      @update:modelValue="emit('update:modelValue', $event || undefined)"
      :placeholder="placeholder"
      :clearable="clearable"
    >
      <ElOption
        v-for="tax in generalTaxes"
        :key="tax.id"
        :label="tax.title"
        :value="tax.id"
      />
    </ElSelect>
  </SaInputLoader>
</template>

<script lang="ts" setup>
import SaInputLoader from '@/components/SaInputLoader.vue';
import { consumeAllPages, generalTaxesApi } from '@/services/api';
import { useValueLoadedByCurrentWorkspace } from '@/services/utils';

defineProps<{
  modelValue?: number;
  placeholder?: string;
  clearable?: boolean;
}>();

const emit = defineEmits<{ (e: 'update:modelValue', value: number): void }>();

const { value: generalTaxes, loading } = useValueLoadedByCurrentWorkspace((workspaceId) =>
  consumeAllPages((pageRequest) =>
    generalTaxesApi.getTaxes({
      ...pageRequest,
      workspaceId,
    }),
  ),
);
</script>
