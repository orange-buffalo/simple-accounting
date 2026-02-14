<template>
  <SaInputLoader :loading="loading">
    <ElSelect
      :model-value="modelValue"
      @update:modelValue="emit('update:modelValue', $event || undefined)"
      :placeholder="placeholder"
      :clearable="clearable"
    >
      <ElOption
        v-for="customer in customers"
        :key="customer.id"
        :label="customer.name"
        :value="customer.id"
      />
    </ElSelect>
  </SaInputLoader>
</template>

<script lang="ts" setup>
import SaInputLoader from '@/components/SaInputLoader.vue';
import { consumeAllPages, customersApi } from '@/services/api';
import { useValueLoadedByCurrentWorkspace } from '@/services/utils';

defineProps<{
  modelValue?: number;
  placeholder?: string;
  clearable?: boolean;
}>();

const emit = defineEmits<{ (e: 'update:modelValue', value: number): void }>();

const { value: customers, loading } = useValueLoadedByCurrentWorkspace((workspaceId) =>
  consumeAllPages((pageRequest) =>
    customersApi.getCustomers({
      ...pageRequest,
      workspaceId,
    }),
  ),
);
</script>
