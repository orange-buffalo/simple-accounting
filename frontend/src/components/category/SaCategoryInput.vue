<template>
  <SaInputLoader :loading="!categoriesLoaded">
    <!--suppress RequiredAttributes -->
    <ElSelect
      v-bind="props"
      v-on="listeners"
    >
      <ElOption
        v-for="category in categories"
        :key="category.id"
        :label="category.name"
        :value="category.id"
      />
    </ElSelect>
  </SaInputLoader>
</template>

<script>
  import useCategories from '@/components/category/useCategories';
  import SaInputLoader from '@/components/SaInputLoader';

  export default {
    components: {
      SaInputLoader,
    },

    props: {
      value: {
        type: Number,
        default: null,
      },
      placeholder: {
        type: String,
        default: null,
      },
    },

    setup(props, { listeners }) {
      const { categories, categoriesLoaded } = useCategories();

      return {
        categories,
        categoriesLoaded,
        props,
        listeners,
      };
    },
  };
</script>
