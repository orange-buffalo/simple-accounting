<template>
  <SaInputLoader :loading="loading">
    <ElSelect
      :model-value="modelValue"
      @update:modelValue="emit('update:modelValue', $event || undefined)"
      :placeholder="placeholder"
      :clearable="clearable"
    >
      <ElOption
        v-for="category in categories"
        :key="category.id"
        :label="category.name"
        :value="category.id"
      />
    </ElSelect>
  </SaInputLoader>
</template>

<script lang="ts" setup>
import SaInputLoader from '@/components/SaInputLoader.vue';
import { categoriesApi, consumeAllPages } from '@/services/api';
import { useValueLoadedByCurrentWorkspace } from '@/services/utils';

defineProps<{
  modelValue?: number;
  placeholder?: string;
  clearable?: boolean;
}>();

const emit = defineEmits<(e: 'update:modelValue', value: number) => void>();

const { value: categories, loading } = useValueLoadedByCurrentWorkspace((workspaceId) =>
  consumeAllPages((pageRequest) =>
    categoriesApi.getCategories({
      ...pageRequest,
      workspaceId,
    }),
  ),
);
</script>
