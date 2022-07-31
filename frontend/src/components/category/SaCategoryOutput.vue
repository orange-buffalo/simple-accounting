<template>
  <SaOutputLoader :loading="loading">
    {{ categoryName }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
  import SaOutputLoader from '@/components/SaOutputLoader.vue';
  import { categoryApi } from '@/services/api';
  import { findByIdOrEmpty, useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';

  const props = defineProps({
    categoryId: {
      type: Number,
      default: null,
    },
  });

  const {
    loading,
    value: categoryName,
  } = useValueLoadedByCurrentWorkspaceAndProp(() => props.categoryId, async (categoryId, workspaceId) => {
    // TODO support GET by id
    const categories = await categoryApi.getCategories({
      workspaceId,
    });
    const category = findByIdOrEmpty(categories.data, categoryId);
    return category?.name;
  });
</script>
