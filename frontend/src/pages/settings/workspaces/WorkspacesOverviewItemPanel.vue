<template>
  <SaOverviewItem :title="workspace.name">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute icon="multi-currency" :tooltip="$t.workspacesOverviewItemPanel.defaultCurrency()">
        {{ workspace.defaultCurrency }}
      </SaOverviewItemPrimaryAttribute>
      <SaOverviewItemPrimaryAttribute icon="globe" :tooltip="$t.workspacesOverviewItemPanel.residency()">
        {{ getCountryName(workspace.residency) }}
      </SaOverviewItemPrimaryAttribute>
    </template>
    <template #middle-column>
      <ElButton v-if="!isCurrent" link @click="switchToWorkspace">
        {{ $t.workspacesOverviewItemPanel.switchToThisWorkspace() }}
      </ElButton>
    </template>
    <template #last-column>
      <SaActionMenu :label="$t.workspacesOverviewItemPanel.actions()">
        <ElButton
          link
          class="workspace-panel__action"
          @click="navigateToWorkspaceEdit"
        >
          <SaIcon icon="pencil" />
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
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import SaActionMenu from '@/components/SaActionMenu.vue';
  import { useCurrentWorkspace, useWorkspaces } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import { getCountryName } from '@/services/i18n/countries';
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
  .workspace-panel__action {
    .sa-icon {
      margin-right: 4px;
    }
  }
</style>
