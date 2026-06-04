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
        <SaActionMenu :label="$t.workspacesOverviewItemPanel.actions()">
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
        </SaActionMenu>
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
  import WorkspacesAttributeValue from '@/pages/settings/workspaces/WorkspacesAttributeValue.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import SaActionMenu from '@/components/SaActionMenu.vue';
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

    &__action {
      .sa-icon {
        margin-right: 4px;
      }
    }
  }
</style>
