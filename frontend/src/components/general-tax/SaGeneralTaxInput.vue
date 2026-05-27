<template>
  <SaEntitySelect
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :options-provider="optionsProvider"
    :option-provider="optionProvider"
    :label-provider="generalTaxLabelProvider"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>

<script lang="ts" setup>
  import SaEntitySelect from '@/components/entity-select/SaEntitySelect.vue';
  import { graphql } from '@/services/api/gql';
  import type { ApiConnectionRequest, HasOptionalId } from '@/services/api';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { useCurrentWorkspace } from '@/services/workspaces';

  defineProps<{
    modelValue?: string,
    placeholder?: string,
    clearable?: boolean,
  }>();

  const emit = defineEmits<{(e: 'update:modelValue', value?: string): void }>();

  type GeneralTaxItem = HasOptionalId & {
    title: string,
  };

  const generalTax = (entity: HasOptionalId) => entity as GeneralTaxItem;
  const generalTaxLabelProvider = (entity: HasOptionalId) => generalTax(entity).title;

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getGeneralTaxesQuery = useLazyQuery(graphql(`
    query getGeneralTaxesForSelect($workspaceId: String!, $first: Int!, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        generalTaxes(first: $first, freeSearchText: $freeSearchText) {
          edges {
            node {
              id
              title
            }
          }
          totalCount
        }
      }
    }
  `), 'workspace');

  const getGeneralTaxQuery = useLazyQuery(graphql(`
    query getGeneralTaxForSelect($workspaceId: String!, $generalTaxId: String!) {
      workspace(id: $workspaceId) {
        generalTax(id: $generalTaxId) {
          id
          title
        }
      }
    }
  `), 'workspace');

  const optionsProvider = async (
    connectionRequest: ApiConnectionRequest,
    query: string | undefined,
    requestInit: RequestInit,
  ) => {
    const workspace = await getGeneralTaxesQuery({
      workspaceId: currentWorkspaceId,
      first: connectionRequest.first ?? 10,
      freeSearchText: query ?? null,
    }, {
      requestConfig: requestInit,
    });
    const edges = workspace?.generalTaxes.edges ?? [];
    return {
      edges: edges.map(({ node }) => ({
        node: { ...node } as GeneralTaxItem,
      })),
      totalCount: workspace?.generalTaxes.totalCount ?? 0,
    };
  };

  const optionProvider = async (
    id: string,
    requestInit: RequestInit,
  ) => {
    const workspace = await getGeneralTaxQuery({
      workspaceId: currentWorkspaceId,
      generalTaxId: id,
    }, {
      requestConfig: requestInit,
    });
    const foundGeneralTax = workspace?.generalTax;
    if (!foundGeneralTax) throw new Error(`General tax ${id} not found`);
    return {
      id: foundGeneralTax.id,
      title: foundGeneralTax.title,
    } as GeneralTaxItem;
  };
</script>
