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
  import { useValueLoadedByCurrentWorkspace } from '@/services/utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  defineProps<{
    modelValue?: number,
    placeholder?: string,
    clearable?: boolean,
  }>();

  const emit = defineEmits<{(e: 'update:modelValue', value: number): void }>();

  const getCategoriesQuery = useLazyQuery(graphql(`
    query getCategoriesForInput($workspaceId: Long!) {
      workspace(id: $workspaceId) {
        categories(first: 500) {
          edges {
            node {
              id
              name
            }
          }
        }
      }
    }
  `), 'workspace');

  const {
    value: categories,
    loading,
  } = useValueLoadedByCurrentWorkspace(async (workspaceId) => {
    const workspace = await getCategoriesQuery({ workspaceId });
    return workspace?.categories.edges.map((edge) => edge.node).sort((a, b) => a.name.localeCompare(b.name)) ?? [];
  });
</script>
