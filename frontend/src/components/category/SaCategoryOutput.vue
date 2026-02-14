<template>
  <SaOutputLoader :loading="loading">
    {{ categoryName }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
import SaOutputLoader from '@/components/SaOutputLoader.vue';
import { categoriesApi, consumeAllPages } from '@/services/api';
import { findByIdOrEmpty, useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';

const props = defineProps<{
  categoryId?: number;
}>();

const { loading, value: categoryName } = useValueLoadedByCurrentWorkspaceAndProp(
  () => [props.categoryId],
  async (_, workspaceId) => {
    if (props.categoryId === undefined) {
      // todo i18n
      return 'Not specified';
    }

    // TODO support GET by id
    const categories = await consumeAllPages((pageRequest) =>
      categoriesApi.getCategories({
        workspaceId,
        ...pageRequest,
      }),
    );
    const category = findByIdOrEmpty(categories, props.categoryId);
    return category?.name;
  },
);
</script>
