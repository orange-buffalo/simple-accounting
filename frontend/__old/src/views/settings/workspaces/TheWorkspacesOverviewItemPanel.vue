<template>
  <div class="workspace-panel">
    <div class="workspace-panel__info-panel">
      <div class="sa-item-title-panel">
        <div class="workspace-panel__info-panel__name">
          <h3>{{ workspace.name }}</h3>
          <ElButton
            v-if="!isCurrent"
            type="text"
            @click="switchToWorkspace"
          >
            Switch to this workspace
          </ElButton>
        </div>
        <span class="sa-item-edit-link">
          <SaIcon icon="pencil-solid" />
          <ElButton
            type="text"
            @click="navigateToWorkspaceEdit"
          >Edit</ElButton>
        </span>
      </div>

      <div class="sa-item-attributes">
        <SaAttributeValue label="Default Currency">
          {{ workspace.defaultCurrency }}
        </SaAttributeValue>
        <br>
        <SaAttributeValue label="Workspace Shares">
          <ElTable
            v-if="hasAccessTokens"
            :data="accessTokens"
          >
            <ElTableColumn
              label="Valid Till"
              #default="{row}"
            >
              {{ $t.common.dateTime.medium(row.validTill) }}
            </ElTableColumn>
            <ElTableColumn align="right" #default="{row}">
              <div class="workspace-panel__share-link-panel">
                <SaIcon icon="copy" />
                <ElButton
                  type="text"
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
              type="text"
              @click="shareWorkspace"
            >
              Create share link
            </ElButton>
          </div>
        </SaAttributeValue>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import copy from 'copy-to-clipboard';
  import {
    computed, defineComponent, PropType, ref,
  } from '@vue/composition-api';
  import SaAttributeValue from '@/components/SaAttributeValue';
  import SaIcon from '@/components/SaIcon';
  import { useCurrentWorkspace, useWorkspaces } from '@/services/workspaces';
  import { apiClient, WorkspaceAccessTokenDto, WorkspaceDto } from '@/services/api';
  import useNavigation from '@/components/navigation/useNavigation';

  export default defineComponent({
    components: {
      SaIcon,
      SaAttributeValue,
    },

    props: {
      workspace: {
        type: Object as PropType<WorkspaceDto>,
        required: true,
      },
    },

    setup(props) {
      const accessTokens = ref<WorkspaceAccessTokenDto[]>([]);
      const newShareValidTill = ref(new Date());

      const hasAccessTokens = computed(() => accessTokens.value.length);

      const { currentWorkspaceId } = useCurrentWorkspace();
      const isCurrent = computed(() => props.workspace.id === currentWorkspaceId);

      const reloadAccessTokens = async () => {
        const response = await apiClient.getAccessTokens({
          workspaceId: props.workspace.id!,
        });
        accessTokens.value = response.data.data;
      };
      reloadAccessTokens();

      const { navigateToView, navigateByPath } = useNavigation();
      const navigateToWorkspaceEdit = () => navigateToView({
        name: 'edit-workspace',
        params: { id: props.workspace.id },
      });

      const switchToWorkspace = () => {
        useWorkspaces()
          .setCurrentWorkspace(props.workspace);
        navigateByPath('/');
      };

      const shareWorkspace = async () => {
        await apiClient.createAccessToken({
          workspaceId: props.workspace.id!,
        }, {
          validTill: newShareValidTill.value.toISOString(),
        });
        await reloadAccessTokens();
      };

      const copyShareLink = (token: string) => {
        const shareLink = `${window.location.origin}/login-by-link/${token}`;
        copy(shareLink);
      };

      return {
        accessTokens,
        newShareValidTill,
        hasAccessTokens,
        isCurrent,
        navigateToWorkspaceEdit,
        switchToWorkspace,
        shareWorkspace,
        copyShareLink,
      };
    },
  });
</script>

<style lang="scss">
  @import "~@/styles/main.scss";
  @import "~@/styles/vars.scss";

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
      .sa-icon {
        margin-right: 3px;
        color: $components-color;
      }
    }
  }
</style>
