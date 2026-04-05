<template>
  <SaOutputLoader :loading="loading">
    {{ categoryName }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
  import SaOutputLoader from '@/components/SaOutputLoader.vue';
  import { useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    categoryId?: number
  }>();

  const getCategoryQuery = useLazyQuery(graphql(`
    query getCategoryForOutput($workspaceId: Long!, $categoryId: Long!) {
      workspace(id: $workspaceId) {
        category(id: $categoryId) {
          name
        }
      }
    }
  `), 'workspace');

  const {
    loading,
    value: categoryName,
  } = useValueLoadedByCurrentWorkspaceAndProp(
    () => [props.categoryId],
    async (_, workspaceId) => {
      if (props.categoryId === undefined) {
        // todo i18n
        return 'Not specified';
      }

      const workspace = await getCategoryQuery({
        workspaceId,
        categoryId: props.categoryId,
      });
      return workspace?.category?.name;
    },
  );
</script>
