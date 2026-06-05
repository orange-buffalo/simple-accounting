<template>
  <SaPage :header="$t.workspaceAccessTokens.header(workspaceName)">
    <SaForm
      v-model="formValues"
      :on-submit="shareWorkspace"
      class="workspace-access-tokens__create-section"
    >
      <h2>{{ $t.workspaceAccessTokens.createNewLink() }}</h2>

      <SaFormItemInternal
        prop="validTill"
        :label="$t.workspaceAccessTokens.validTill()"
      >
        <ElDatePicker
          v-model="formValues.validTill"
          type="datetime"
          :placeholder="$t.workspaceAccessTokens.validTillPlaceholder()"
        />
      </SaFormItemInternal>
    </SaForm>

    <section
      v-if="accessTokens.length > 0"
      class="workspace-access-tokens__section workspace-access-tokens__manage-section"
    >
      <h2>{{ $t.workspaceAccessTokens.manageExistingLinks() }}</h2>

      <ElTable
        :data="accessTokens"
        class="workspace-access-tokens__table"
      >
        <ElTableColumn
          :label="$t.workspaceAccessTokens.link()"
          min-width="360"
          #default="{ row }"
        >
          <ElTooltip
            :content="$t.workspaceAccessTokens.clickToCopy()"
            placement="bottom"
          >
            <ElButton
              link
              class="workspace-access-tokens__copy-link"
              @click="copyShareLink(row.token)"
            >
              <SaIcon icon="copy" />
              <span class="workspace-access-tokens__link-text">
                {{ shareLink(row.token) }}
              </span>
            </ElButton>
          </ElTooltip>
        </ElTableColumn>
        <ElTableColumn
          :label="$t.workspaceAccessTokens.validTill()"
          width="190"
          class-name="workspace-access-tokens__valid-till-column"
          #default="{ row }"
        >
          {{ $t.common.dateTime.medium(row.validTill) }}
        </ElTableColumn>
        <ElTableColumn align="right" width="120" #default="{ row }">
          <div class="workspace-access-tokens__actions">
            <ElButton
              link
              type="danger"
              class="workspace-access-tokens__revoke-action"
              @click="revokeAccessToken(row.id)"
            >
              <Delete class="workspace-access-tokens__delete-icon" />
              {{ $t.workspaceAccessTokens.revoke() }}
            </ElButton>
          </div>
        </ElTableColumn>
      </ElTable>
    </section>
  </SaPage>
</template>

<script lang="ts" setup>
  import copy from 'copy-to-clipboard';
  import { ref } from 'vue';
  import { Delete } from '@element-plus/icons-vue';
  import { ElDatePicker, ElTooltip } from 'element-plus';
  import SaPage from '@/components/SaPage.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import { useConfirmation } from '@/components/confirmation/use-confirmation';
  import useNotifications from '@/components/notifications/use-notifications';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation } from '@/services/api/use-gql-api';
  import { $t } from '@/services/i18n';

  const props = defineProps<{
    id: string,
  }>();

  const workspaceAccessTokensQuery = graphql(`
    query workspaceAccessTokensPage($workspaceId: String!, $first: Int!) {
      workspace(id: $workspaceId) {
        name
        workspaceAccessTokens(first: $first) {
          edges {
            node {
              id
              validTill
              token
            }
          }
        }
      }
    }
  `);

  const createWorkspaceAccessTokenMutation = graphql(`
    mutation createWorkspaceAccessTokenPage($workspaceId: String!, $validTill: DateTime!) {
      createWorkspaceAccessToken(workspaceId: $workspaceId, validTill: $validTill) {
        id
        token
        validTill
      }
    }
  `);

  const revokeWorkspaceAccessTokenMutation = graphql(`
    mutation revokeWorkspaceAccessTokenPage($accessTokenId: String!) {
      revokeWorkspaceAccessToken(accessTokenId: $accessTokenId)
    }
  `);

  const loadAccessTokens = useLazyQuery(workspaceAccessTokensQuery, 'workspace');
  const executeCreateToken = useMutation(createWorkspaceAccessTokenMutation, 'createWorkspaceAccessToken');
  const executeRevokeToken = useMutation(revokeWorkspaceAccessTokenMutation, 'revokeWorkspaceAccessToken');
  const { showSuccessNotification } = useNotifications();

  type AccessTokenRow = { id: string; validTill: string; token: string };
  const workspaceName = ref('');
  const accessTokens = ref<AccessTokenRow[]>([]);
  const formValues = ref({
    validTill: new Date(),
  });

  const reloadAccessTokens = async () => {
    const result = await loadAccessTokens({
      workspaceId: props.id,
      first: 100,
    });
    workspaceName.value = result.name;
    accessTokens.value = result.workspaceAccessTokens.edges.map((edge) => ({
      id: edge.node.id,
      validTill: edge.node.validTill,
      token: edge.node.token,
    }));
  };
  reloadAccessTokens();

  const shareWorkspace = async () => {
    await executeCreateToken({
      workspaceId: props.id,
      validTill: formValues.value.validTill.toISOString(),
    });
    await reloadAccessTokens();
    showSuccessNotification($t.value.workspaceAccessTokens.linkCreated());
  };

  const accessTokenIdToRevoke = ref<string>();
  const confirmRevokeAccessToken = useConfirmation(
    $t.value.workspaceAccessTokens.revokeConfirm.message(),
    {
      title: $t.value.workspaceAccessTokens.revokeConfirm.title(),
      confirmButtonText: $t.value.workspaceAccessTokens.revokeConfirm.yes(),
      cancelButtonText: $t.value.workspaceAccessTokens.revokeConfirm.no(),
      type: 'warning',
      customClass: 'workspace-access-tokens__revoke-confirmation',
      modalClass: 'workspace-access-tokens__revoke-confirmation-overlay',
    },
    async () => {
      if (!accessTokenIdToRevoke.value) return;
      await executeRevokeToken({ accessTokenId: accessTokenIdToRevoke.value });
      await reloadAccessTokens();
    },
  );

  const revokeAccessToken = async (accessTokenId: string) => {
    accessTokenIdToRevoke.value = accessTokenId;
    await confirmRevokeAccessToken();
  };

  const shareLink = (token: string) => `${window.location.origin}/login-by-link/${token}`;

  const copyShareLink = (token: string) => {
    copy(shareLink(token));
    showSuccessNotification($t.value.workspaceAccessTokens.linkCopied());
  };
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .workspace-access-tokens {
    &__section {
      @include white-panel;
      margin-bottom: 30px;

      h2 {
        margin-top: 0;
      }
    }

    &__actions {
      display: inline-flex;
      gap: 12px;
    }

    &__manage-section,
    &__table {
      position: relative;
      z-index: 0;
    }

    &__valid-till-column {
      white-space: nowrap;
    }

    &__revoke-action.el-button.is-link {
      color: var(--el-color-danger) !important;

      &:hover,
      &:focus {
        color: var(--el-color-danger-light-3) !important;
      }
    }

    &__delete-icon {
      width: 1em;
      height: 1em;
      margin-right: 4px;
    }

    &__copy-link {
      max-width: 100%;

      > span {
        display: inline-flex;
        max-width: 100%;
        min-width: 0;
        align-items: center;
      }

      .sa-icon {
        flex: 0 0 auto;
        margin-right: 4px;
        color: $components-color;
      }
    }

    &__link-text {
      align-items: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .workspace-access-tokens__revoke-confirmation.el-message-box {
    position: relative;
    z-index: 3000 !important;
  }

  .workspace-access-tokens__revoke-confirmation-overlay.el-overlay {
    z-index: 3000 !important;
  }
</style>
