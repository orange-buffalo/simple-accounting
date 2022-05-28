<template>
  <SaOutputLoader :loading="!categoriesLoaded">
    {{ categoryName }}
  </SaOutputLoader>
</template>

<script lang="ts">
  import { computed, defineComponent } from '@vue/composition-api';
  import useCategories from '@/components/category/useCategories';
  import SaOutputLoader from '@/components/SaOutputLoader';

  export default defineComponent({
    components: {
      SaOutputLoader,
    },

    props: {
      categoryId: {
        type: Number,
        default: null,
      },
    },

    setup(props) {
      const { categoryById, categoriesLoaded } = useCategories();

      const categoryName = computed(() => categoryById.value(props.categoryId).name);

      return {
        categoryName,
        categoriesLoaded,
      };
    },
  });
</script>
