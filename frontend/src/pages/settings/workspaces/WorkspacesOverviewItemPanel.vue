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
            Switch to this workspace
          </ElButton>
        </div>
        <span class="sa-item-edit-link">
          <SaIcon icon="pencil-solid" />
          <ElButton
            link
            @click="navigateToWorkspaceEdit"
          >Edit</ElButton>
        </span>
      </div>

      <div class="sa-item-attributes">
        <WorkspacesAttributeValue label="Default Currency">
          {{ workspace.defaultCurrency }}
        </WorkspacesAttributeValue>
        <br>
        <WorkspacesAttributeValue label="Workspace Shares">
          <ElTable
            v-if="hasAccessTokens"
            :data="accessTokens"
          >
            <ElTableColumn
              label="Valid Till"
              #default="{ row }"
            >
              {{ $t.common.dateTime.medium(row.validTill) }}
            </ElTableColumn>
            <ElTableColumn align="right" #default="{ row }">
              <div class="workspace-panel__share-link-panel">
                <SaIcon icon="copy" />
                <ElButton
                  link
                  @click="copyShareLink(row.token)"
                >
                  Copy link
                </ElButton>
              </div>
            </ElTableColumn>
          </ElTable>

          <div class="workspace-panel__create-share-panel">
            {{ hasAccessTokens ? 'Add another share valid till' : 'Start sharing workspace, new link valid till' }}:
            <ElDatePicker
              v-model="newShareValidTill"
              type="datetime"
              placeholder="Link valid till"
            />
            <SaIcon icon="share" />
            <ElButton
              link
              @click="shareWorkspace"
            >
              Create share link
            </ElButton>
          </div>
        </WorkspacesAttributeValue>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import copy from 'copy-to-clipboard';
import { computed, ref } from 'vue';
import SaIcon from '@/components/SaIcon.vue';
import WorkspacesAttributeValue from '@/pages/settings/workspaces/WorkspacesAttributeValue.vue';
import type { WorkspaceAccessTokenDto, WorkspaceDto } from '@/services/api';
import { workspaceAccessTokensApi } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { ensureDefined } from '@/services/utils';
import { useCurrentWorkspace, useWorkspaces } from '@/services/workspaces';

const props = defineProps<{
  workspace: WorkspaceDto;
}>();

const accessTokens = ref<WorkspaceAccessTokenDto[]>([]);
const newShareValidTill = ref(new Date());

const hasAccessTokens = computed(() => accessTokens.value.length);

const { currentWorkspaceId } = useCurrentWorkspace();
const isCurrent = computed(() => props.workspace.id === currentWorkspaceId);

const reloadAccessTokens = async () => {
  // TODO #463: consumeAllPages
  const response = await workspaceAccessTokensApi.getAccessTokens({
    workspaceId: ensureDefined(props.workspace.id),
  });
  accessTokens.value = response.data;
};
reloadAccessTokens();

const { navigateToView, navigateByPath } = useNavigation();
const navigateToWorkspaceEdit = () =>
  navigateToView({
    name: 'edit-workspace',
    params: { id: props.workspace.id },
  });

const switchToWorkspace = () => {
  useWorkspaces().setCurrentWorkspace(props.workspace);
  navigateByPath('/');
};

const shareWorkspace = async () => {
  await workspaceAccessTokensApi.createAccessToken({
    workspaceId: ensureDefined(props.workspace.id),
    createWorkspaceAccessTokenDto: {
      validTill: newShareValidTill.value.toISOString(),
    },
  });
  await reloadAccessTokens();
};

const copyShareLink = (token: string) => {
  const shareLink = `${window.location.origin}/login-by-link/${token}`;
  copy(shareLink);
};
</script>

<style lang="scss">
  @use "@/styles/main.scss" as *;
  @use "@/styles/vars.scss" as *;

  .workspace-panel {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;

    &__info-panel {
      @extend .sa-item-info-panel;
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

    .el-table {
      margin-bottom: 10px;
    }

    &__create-share-panel {
      display: flex;
      align-items: center;

      .el-date-editor {
        margin: 0 10px;
      }

      .sa-icon {
        margin-right: 3px;
        color: $components-color;
      }
    }

    &__share-link-panel {
      display: inline-flex;
      align-items: center;

      .sa-icon {
        margin-right: 3px;
        color: $components-color;
      }

      .el-button {
        padding: 0;
      }
    }
  }
</style>
