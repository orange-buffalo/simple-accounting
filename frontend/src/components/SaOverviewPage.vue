<template>
  <SaPage>
    <template #header>
      <div class="sa-overview-page__header-row">
        <h1>{{ header }}</h1>
        <ElButton
          v-if="createActionVisible"
          class="sa-overview-page__create-action"
          link
          :disabled="createActionDisabled"
          @click="navigateToCreateAction"
        >
          <SaIcon icon="plus-thin" />
          {{ createActionLabel }}
        </ElButton>
      </div>
    </template>
    <div v-if="filters && Object.keys(filters).length > 0" class="sa-overview-page__content-options">
      <SaOverviewFilters :model-value="modelValue" :filters="filters" @update:model-value="emit('update:modelValue', $event)" />
      <div class="sa-overview-page__actions"><slot name="actions" /></div>
    </div>
    <slot />
  </SaPage>
</template>

<script lang="ts" setup generic="TFilters extends OverviewFilters">
  import { computed } from 'vue';
  import SaPage from '@/components/SaPage.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import SaOverviewFilters from '@/components/overview-page/SaOverviewFilters.vue';
  import useNavigation from '@/services/use-navigation';
  import type {
    SaOverviewFilterConfigs, SaOverviewFilters as OverviewFilters,
  } from '@/components/overview-page/overview-page-filters';

  const props = withDefaults(defineProps<{
    header: string,
    modelValue?: TFilters,
    filters?: SaOverviewFilterConfigs<TFilters>,
    createActionAvailable?: boolean,
    createActionDisabled?: boolean,
    createActionLabel?: string,
    createActionViewName?: string,
  }>(), { createActionAvailable: true, createActionDisabled: false });
  const emit = defineEmits<{ 'update:modelValue': [value: TFilters] }>();
  const { navigateByViewName } = useNavigation();
  const createActionVisible = computed(() => props.createActionAvailable
    && props.createActionLabel != null && props.createActionViewName != null);
  const navigateToCreateAction = () => navigateByViewName(props.createActionViewName!);
</script>

<style lang="scss" scoped>
  .sa-overview-page {
    &__header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 10px;
      h1 { margin: 0; font-size: 160%; }
    }
    &__create-action { :deep(span) { gap: 4px; } }
    &__content-options { display: flex; width: 100%; margin-bottom: 24px; }
    &__actions { margin-left: auto; }
  }
</style>
