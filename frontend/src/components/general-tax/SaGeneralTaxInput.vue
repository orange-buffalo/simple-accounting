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
  import { useValueLoadedByCurrentWorkspace } from '@/services/utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  defineProps<{
    modelValue?: number,
    placeholder?: string,
    clearable?: boolean,
  }>();

  const emit = defineEmits<{(e: 'update:modelValue', value: number): void }>();

  const getGeneralTaxesQuery = useLazyQuery(graphql(`
    query getGeneralTaxesForInput($workspaceId: Long!) {
      workspace(id: $workspaceId) {
        generalTaxes(first: 500) {
          edges {
            node {
              id
              title
            }
          }
        }
      }
    }
  `), 'workspace');

  const {
    value: generalTaxes,
    loading,
  } = useValueLoadedByCurrentWorkspace(async (workspaceId) => {
    const workspace = await getGeneralTaxesQuery({ workspaceId });
    return workspace?.generalTaxes.edges.map((edge) => edge.node).sort((a, b) => a.title.localeCompare(b.title)) ?? [];
  });
</script>
