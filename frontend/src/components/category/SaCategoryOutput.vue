<template>
  <SaOutputLoader :loading="loading">
    {{ categoryName }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
  import SaOutputLoader from '@/components/SaOutputLoader.vue';
  import { categoryApi, consumeAllPages } from '@/services/api';
  import { findByIdOrEmpty, useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';

  const props = defineProps({
    categoryId: {
      type: Number,
      default: null,
    },
    unspecifiedCategory: {
      type: Boolean,
      default: false,
    },
  });

  const {
    loading,
    value: categoryName,
  } = useValueLoadedByCurrentWorkspaceAndProp(
    () => [props.categoryId, props.unspecifiedCategory],
    async (_, workspaceId) => {
      if (props.unspecifiedCategory) {
        // todo i18n
        return 'Not specified';
      }

      // TODO support GET by id
      const categories = await consumeAllPages(((pageRequest) => categoryApi.getCategories({
        workspaceId,
        ...pageRequest,
      })));
      const category = findByIdOrEmpty(categories, props.categoryId);
      return category?.name;
    },
  );
</script>
