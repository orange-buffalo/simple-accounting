<template>
  <SaEntitySelect
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :options-provider="optionsProvider"
    :option-provider="optionProvider"
    :label-provider="customerLabelProvider"
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

  type CustomerItem = HasOptionalId & {
    name: string,
  };

  const customer = (entity: HasOptionalId) => entity as CustomerItem;
  const customerLabelProvider = (entity: HasOptionalId) => customer(entity).name;

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getCustomersQuery = useLazyQuery(graphql(`
    query getCustomersForSelect($workspaceId: String!, $first: Int!, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        customers(first: $first, freeSearchText: $freeSearchText) {
          edges {
            node {
              id
              name
            }
          }
          totalCount
        }
      }
    }
  `), 'workspace');

  const getCustomerQuery = useLazyQuery(graphql(`
    query getCustomerForSelect($workspaceId: String!, $customerId: String!) {
      workspace(id: $workspaceId) {
        customer(id: $customerId) {
          id
          name
        }
      }
    }
  `), 'workspace');

  const optionsProvider = async (
    connectionRequest: ApiConnectionRequest,
    query: string | undefined,
    requestInit: RequestInit,
  ) => {
    const workspace = await getCustomersQuery({
      workspaceId: currentWorkspaceId,
      first: connectionRequest.first ?? 10,
      freeSearchText: query ?? null,
    }, {
      requestConfig: requestInit,
    });
    const edges = workspace?.customers.edges ?? [];
    return {
      edges: edges.map(({ node }) => ({
        node: { ...node } as CustomerItem,
      })),
      totalCount: workspace?.customers.totalCount ?? 0,
    };
  };

  const optionProvider = async (
    id: string,
    requestInit: RequestInit,
  ) => {
    const workspace = await getCustomerQuery({
      workspaceId: currentWorkspaceId,
      customerId: id,
    }, {
      requestConfig: requestInit,
    });
    const foundCustomer = workspace?.customer;
    if (!foundCustomer) throw new Error(`Customer ${id} not found`);
    return {
      id: foundCustomer.id,
      name: foundCustomer.name,
    } as CustomerItem;
  };
</script>
