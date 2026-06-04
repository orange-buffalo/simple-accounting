<template>
  <div class="workspace-panel">
    <div class="workspace-panel__info-panel">
      <div class="sa-item-title-panel">
        <div class="workspace-panel__info-panel__name">
          <h3>{{ workspace.name }}</h3>
          <ElButton
            v-if="!isCurrent"
            link
            @click="switchToWorkspace"
          >
            {{ $t.workspacesOverviewItemPanel.switchToThisWorkspace() }}
          </ElButton>
        </div>
        <div class="workspace-panel__actions">
          <ElPopover
            trigger="click"
            placement="bottom-end"
            popper-class="workspace-panel__actions-popover"
          >
            <template #reference>
              <ElButton
                link
                :icon="Menu"
                :aria-label="$t.workspacesOverviewItemPanel.actions()"
                class="workspace-panel__actions-trigger"
              />
            </template>

            <div class="workspace-panel__actions-menu">
              <ElButton
                link
                class="workspace-panel__action"
                @click="navigateToWorkspaceEdit"
              >
                <SaIcon icon="pencil-solid" />
                {{ $t.workspacesOverviewItemPanel.edit() }}
              </ElButton>
              <ElButton
                link
                class="workspace-panel__action"
                @click="navigateToWorkspaceAccessTokens"
              >
                <SaIcon icon="share" />
                {{ $t.workspacesOverviewItemPanel.manageAccessTokens() }}
              </ElButton>
            </div>
          </ElPopover>
        </div>
      </div>

      <div class="sa-item-attributes">
        <WorkspacesAttributeValue :label="$t.workspacesOverviewItemPanel.defaultCurrency()">
          {{ workspace.defaultCurrency }}
        </WorkspacesAttributeValue>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { ElPopover } from 'element-plus';
  import { Menu } from '@element-plus/icons-vue';
  import WorkspacesAttributeValue from '@/pages/settings/workspaces/WorkspacesAttributeValue.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import { useCurrentWorkspace, useWorkspaces } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import type { WorkspacesPageQuery } from '@/services/api/gql/graphql';

  type WorkspaceNode = WorkspacesPageQuery['workspaces']['edges'][0]['node'];

  const props = defineProps<{
    workspace: WorkspaceNode,
  }>();

  const { currentWorkspaceId } = useCurrentWorkspace();
  const isCurrent = computed(() => props.workspace.id === currentWorkspaceId);

  const {
    navigateToView,
    navigateByPath,
  } = useNavigation();
  const navigateToWorkspaceEdit = () => navigateToView({
    name: 'edit-workspace',
    params: { id: props.workspace.id },
  });

  const navigateToWorkspaceAccessTokens = () => navigateToView({
    name: 'workspace-access-tokens',
    params: { id: props.workspace.id },
  });

  const switchToWorkspace = () => {
    useWorkspaces()
      .setCurrentWorkspace({
        id: props.workspace.id,
        name: props.workspace.name,
        defaultCurrency: props.workspace.defaultCurrency,
        editable: true,
      });
    navigateByPath('/');
  };

</script>

<style lang="scss">
  @use "@/styles/mixins.scss" as *;
  @use "@/styles/vars.scss" as *;

  .workspace-panel {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;

    &__info-panel {
      @include item-info-panel;
      border-radius: 2px 1px 1px 2px;
      flex-grow: 1;

      &__name {
        h3 {
          display: inline-block;
        }
      }

      .sa-item-title-panel {
        h3 {
          margin-right: 10px;
        }
      }
    }

    &__actions {
      display: flex;
      justify-content: flex-end;
    }

    &__action {
      .sa-icon {
        margin-right: 4px;
      }
    }
  }

  .workspace-panel__actions-popover {
    width: max-content !important;
    min-width: 220px !important;
    max-width: calc(100vw - 32px) !important;
  }

  .workspace-panel__actions-menu {
    display: inline-flex;
    flex-direction: column;
    align-items: stretch;
    gap: 5px;
    min-width: 220px;
    width: max-content;
    max-width: calc(100vw - 64px);

    .el-button {
      display: flex;
      justify-content: flex-start;
      margin-left: 0;
      white-space: nowrap;
      width: 100%;

      > span {
        display: inline-flex;
        align-items: center;
        justify-content: flex-start;
        width: 100%;
      }

      .sa-icon {
        margin-right: 4px;
        color: $components-color;
      }
    }
  }
</style>
