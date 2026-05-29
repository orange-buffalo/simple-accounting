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

    <template #header-options>
      <div class="sa-overview-page__header-options">
        <div>
          <span>{{ $t.overviewPage.filters.announcement() }}</span>
        </div>

        <div v-if="filterPlaceholder">
          <ElInput
            class="sa-overview-page__filter-input"
            :model-value="modelValue"
            :placeholder="filterPlaceholder"
            clearable
            @update:model-value="emit('update:modelValue', $event)"
          >
            <template #prefix>
              <Search class="sa-overview-page__filter-input__icon" />
            </template>
          </ElInput>
        </div>

        <div class="sa-overview-page__actions">
          <slot name="actions" />
        </div>
      </div>
    </template>

    <slot />
  </SaPage>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { Search } from '@element-plus/icons-vue';
  import SaPage from '@/components/SaPage.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';

  const props = withDefaults(defineProps<{
    header: string,
    filterPlaceholder?: string,
    modelValue?: string,
    createActionAvailable?: boolean,
    createActionDisabled?: boolean,
    createActionLabel?: string,
    createActionViewName?: string,
  }>(), {
    createActionAvailable: true,
    createActionDisabled: false,
  });

  const emit = defineEmits<{
    'update:modelValue': [value: string | undefined],
  }>();

  const { navigateByViewName } = useNavigation();
  const createActionVisible = computed(() => props.createActionAvailable
    && props.createActionLabel != null
    && props.createActionViewName != null);
  const navigateToCreateAction = () => navigateByViewName(props.createActionViewName!);
</script>

<style lang="scss" scoped>
  @use "@/styles/vars.scss" as *;

  .sa-overview-page {
    &__header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 10px;

      h1 {
        margin: 0;
        font-size: 160%;
      }
    }

    &__create-action {
      :deep(span) {
        gap: 4px;
      }
    }

    &__header-options {
      display: grid;
      grid-template-columns: 1fr auto 1fr;
      align-items: center;
      width: 100%;
    }

    &__actions {
      justify-self: end;
    }

    &__filter-input {
      :deep(.el-input__wrapper) {
        background-color: transparent;
        border: none;
        box-shadow: none;
        color: $secondary-text-color;
        max-width: 200px;
      }

      &__icon {
        width: 16px;
        height: 16px;
      }
    }
  }
</style>
