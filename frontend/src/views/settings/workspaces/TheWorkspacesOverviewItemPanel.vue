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
          <Svgicon name="pencil-solid" />
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
            >
              <template slot-scope="scope">
                {{ mediumDateTimeFormatterFromString(scope.row.validTill) }}
              </template>
            </ElTableColumn>
            <ElTableColumn align="right">
              <template slot-scope="scope">
                <div class="workspace-panel__share-link-panel">
                  <Svgicon name="copy" />
                  <ElButton
                    type="text"
                    @click="copyShareLink(scope.row.token)"
                  >
                    Copy link
                  </ElButton>
                </div>
              </template>
            </ElTableColumn>
          </ElTable>

          <div class="workspace-panel__create-share-panel">
            {{ hasAccessTokens ? 'Add another share valid till' : 'Start sharing workspace, new link valid till' }}:
            <ElDatePicker
              v-model="newShareValidTill"
              type="datetime"
              placeholder="Link valid till"
            />
            <Svgicon name="share" />
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

<script>
  import '@/components/icons/pencil-solid';
  import '@/components/icons/share';
  import '@/components/icons/copy';
  import copy from 'copy-to-clipboard';
  import { withWorkspaces } from '@/components/mixins/with-workspaces';
  import SaAttributeValue from '@/components/SaAttributeValue';
  import { api } from '@/services/api';
  import { withMediumDateTimeFormatter } from '@/components/mixins/with-medium-datetime-formatter';

  export default {
    name: 'TheWorkspacesOverviewItemPanel',

    components: {
      SaAttributeValue,
    },

    mixins: [withWorkspaces, withMediumDateTimeFormatter],

    props: {
      workspace: Object,
    },

    data() {
      return {
        accessTokens: [],
        newShareValidTill: new Date(),
      };
    },

    computed: {
      hasAccessTokens() {
        return this.accessTokens.length;
      },

      isCurrent() {
        return this.workspace.id === this.currentWorkspace.id;
      },
    },

    async created() {
      this._reloadAccessTokens();
    },

    methods: {
      navigateToWorkspaceEdit() {
        this.$router.push({ name: 'edit-workspace', params: { id: this.workspace.id } });
      },

      switchToWorkspace() {
        // todo #90: do not commit directly, use wrapper action
        this.$store.commit('workspaces/setCurrentWorkspace', this.workspace);
        this.$router.push('/');
      },

      async shareWorkspace() {
        await api.post(`/workspaces/${this.workspace.id}/workspace-access-tokens`, {
          validTill: this.newShareValidTill.toISOString(),
        });
        this._reloadAccessTokens();
      },

      async _reloadAccessTokens() {
        const response = await api.get(`/workspaces/${this.workspace.id}/workspace-access-tokens`);
        this.accessTokens = response.data.data;
      },

      copyShareLink(token) {
        const shareLink = `${window.location.origin}/login-by-link/${token}`;
        copy(shareLink);
      },
    },
  };
</script>

<style lang="scss">
  @import "@/styles/main.scss";
  @import "@/styles/vars.scss";

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

      .svg-icon {
        margin-right: 3px;
        color: $components-color;
      }
    }

    &__share-link-panel {
      .svg-icon {
        margin-right: 3px;
        color: $components-color;
      }
    }
  }
</style>
