<template>
  <div class="sa-action-menu">
    <ElPopover
      trigger="click"
      placement="bottom-end"
      popper-class="sa-action-menu__popover"
    >
      <template #reference>
        <ElButton
          link
          :icon="Menu"
          :aria-label="label"
          class="sa-action-menu__trigger"
        />
      </template>

      <div class="sa-action-menu__items">
        <slot />
      </div>
    </ElPopover>
  </div>
</template>

<script lang="ts" setup>
  import { ElPopover } from 'element-plus';
  import { Menu } from '@element-plus/icons-vue';

  defineProps<{
    label: string,
  }>();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-action-menu {
    display: flex;
    justify-content: flex-end;

    &__popover {
      width: max-content !important;
      min-width: 220px !important;
      max-width: calc(100vw - 32px) !important;
    }

    &__items {
      display: inline-flex;
      flex-direction: column;
      align-items: stretch;
      gap: 5px;
      min-width: 220px;
      width: max-content;
      max-width: calc(100vw - 64px);

      .el-button,
      .sa-document-download-link {
        display: flex;
        justify-content: flex-start;
        margin-left: 0;
        white-space: nowrap;
        width: 100%;
      }

      .el-button + .el-button {
        margin-left: 0;
      }

      .el-button > span,
      .sa-document-download-link > span {
        display: inline-flex;
        align-items: center;
        justify-content: flex-start;
        width: 100%;
      }

      .sa-icon,
      .el-icon {
        margin-right: 4px;
        color: $components-color;
      }

      .el-button--danger,
      .el-button--danger .el-icon {
        color: var(--el-color-danger);
      }
    }
  }
</style>
