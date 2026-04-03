<template>
  <SaOverviewItem :title="category.name">
    <template #primary-attributes>
      <span v-if="category.description">{{ category.description }}</span>
    </template>
    <template #attributes-preview>
      <span v-if="category.income">Income</span>
      <span v-if="category.income && category.expense"> • </span>
      <span v-if="category.expense">Expense</span>
    </template>
    <template #last-column>
      <SaActionLink
        icon="pencil-solid"
        @click="navigateToCategoryEdit"
      >
        Edit
      </SaActionLink>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaActionLink from '@/components/SaActionLink.vue';
  import useNavigation from '@/services/use-navigation';
  import type { CategoriesPageQuery } from '@/services/api/gql/graphql';

  type CategoryNode = CategoriesPageQuery['workspace']['categories']['edges'][0]['node'];

  const props = defineProps<{
    category: CategoryNode
  }>();

  const { navigateToView } = useNavigation();
  const navigateToCategoryEdit = () => navigateToView({
    name: 'edit-category',
    params: { id: props.category.id },
  });
</script>
