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
  import { useValueLoadedByCurrentWorkspace } from '@/services/utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  defineProps<{
    modelValue?: number,
    placeholder?: string,
    clearable?: boolean,
  }>();

  const emit = defineEmits<{(e: 'update:modelValue', value: number): void }>();

  const getCustomersQuery = useLazyQuery(graphql(`
    query getCustomersForInput($workspaceId: Long!) {
      workspace(id: $workspaceId) {
        customers(first: 500) {
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
    value: customers,
    loading,
  } = useValueLoadedByCurrentWorkspace(async (workspaceId) => {
    const workspace = await getCustomersQuery({ workspaceId });
    return workspace?.customers.edges.map((edge) => edge.node).sort((a, b) => a.name.localeCompare(b.name)) ?? [];
  });
</script>
