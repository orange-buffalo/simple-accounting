<template>
  <SaOverviewItem :title="category.name">
    <template #primary-attributes>
      <span v-if="category.description">{{ category.description }}</span>
    </template>
    <template #attributes-preview>
      <span v-if="category.income">{{ $t.categoriesOverview.type.income() }}</span>
      <span v-if="category.income && category.expense"> • </span>
      <span v-if="category.expense">{{ $t.categoriesOverview.type.expense() }}</span>
    </template>
    <template #last-column>
      <SaActionLink
        icon="pencil-solid"
        @click="navigateToCategoryEdit"
      >
        {{ $t.categoriesOverview.edit() }}
      </SaActionLink>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaActionLink from '@/components/SaActionLink.vue';
  import useNavigation from '@/services/use-navigation';
  import type { CategoriesPageQuery } from '@/services/api/gql/graphql';
  import { $t } from '@/services/i18n';

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
