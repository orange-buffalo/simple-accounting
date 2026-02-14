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
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import type { CategoryDto } from '@/services/api';

  defineProps<{
    category: CategoryDto
  }>();
</script>
