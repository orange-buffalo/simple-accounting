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

      <slot name="actions" />
    </template>

    <slot />
  </SaPage>
</template>

<script lang="ts" setup>
  import { Search } from '@element-plus/icons-vue';
  import SaPage from '@/components/SaPage.vue';
  import { $t } from '@/services/i18n';

  defineProps<{
    header: string,
    filterPlaceholder?: string,
    modelValue?: string,
  }>();

  const emit = defineEmits<{
    'update:modelValue': [value: string | undefined],
  }>();
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
