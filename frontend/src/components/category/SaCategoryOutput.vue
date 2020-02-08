<template>
  <SaOutputLoader :loading="!categoriesLoaded">
    {{ categoryName }}
  </SaOutputLoader>
</template>

<script>
  import { computed } from '@vue/composition-api';
  import useCategories from '@/components/category/useCategories';
  import SaOutputLoader from '@/components/SaOutputLoader';

  export default {
    components: {
      SaOutputLoader,
    },

    props: {
      categoryId: {
        type: Number,
        default: null,
      },
    },

    setup({ categoryId }) {
      const { categoryById, categoriesLoaded } = useCategories();

      const categoryName = computed(() => categoryById.value(categoryId).name);

      return {
        categoryName,
        categoriesLoaded,
      };
    },
  };
</script>
