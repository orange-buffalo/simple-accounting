<template>
  <SaPage :header="header">
    <template #header-options>
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

      <ElButton
        v-if="createActionVisible"
        round
        :disabled="createActionDisabled"
        @click="navigateToCreateAction"
      >
        <SaIcon icon="plus-thin" />
        {{ createActionLabel }}
      </ElButton>

      <slot name="actions" />
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
