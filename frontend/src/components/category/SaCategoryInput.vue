<template>
  <SaEntitySelect
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :options-provider="optionsProvider"
    :option-provider="optionProvider"
    :label-provider="categoryLabelProvider"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>

<script lang="ts" setup>
  import SaEntitySelect from '@/components/entity-select/SaEntitySelect.vue';
  import { graphql } from '@/services/api/gql';
  import type { ApiConnectionRequest, HasOptionalId } from '@/services/api';
  import type { CategoryType } from '@/services/api/gql/graphql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { useCurrentWorkspace } from '@/services/workspaces';

  const props = defineProps<{
    modelValue?: string,
    placeholder?: string,
    clearable?: boolean,
    categoryType?: CategoryType,
  }>();

  const emit = defineEmits<{(e: 'update:modelValue', value?: string): void }>();

  type CategoryItem = HasOptionalId & {
    name: string,
  };

  const category = (entity: HasOptionalId) => entity as CategoryItem;
  const categoryLabelProvider = (entity: HasOptionalId) => category(entity).name;

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getCategoriesQuery = useLazyQuery(graphql(`
    query getCategoriesForSelect(
      $workspaceId: String!,
      $first: Int!,
      $freeSearchText: String,
      $typeIn: [CategoryType!]
    ) {
      workspace(id: $workspaceId) {
        categories(first: $first, freeSearchText: $freeSearchText, typeIn: $typeIn) {
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

  const getCategoryQuery = useLazyQuery(graphql(`
    query getCategoryForSelect($workspaceId: String!, $categoryId: String!) {
      workspace(id: $workspaceId) {
        category(id: $categoryId) {
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
    const workspace = await getCategoriesQuery({
      workspaceId: currentWorkspaceId,
      first: connectionRequest.first ?? 10,
      freeSearchText: query ?? null,
      typeIn: props.categoryType ? [props.categoryType] : null,
    }, {
      requestConfig: requestInit,
    });
    const edges = workspace?.categories.edges ?? [];
    return {
      edges: edges.map(({ node }) => ({
        node: { ...node } as CategoryItem,
      })),
      totalCount: workspace?.categories.totalCount ?? 0,
    };
  };

  const optionProvider = async (
    id: string,
    requestInit: RequestInit,
  ) => {
    const workspace = await getCategoryQuery({
      workspaceId: currentWorkspaceId,
      categoryId: id,
    }, {
      requestConfig: requestInit,
    });
    const foundCategory = workspace?.category;
    if (!foundCategory) throw new Error(`Category ${id} not found`);
    return {
      id: foundCategory.id,
      name: foundCategory.name,
    } as CategoryItem;
  };
</script>
