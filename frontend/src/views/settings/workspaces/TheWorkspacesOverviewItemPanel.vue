<template>
  <div class="workspace-panel">
    <div class="workspace-panel__info-panel">
      <div class="sa-item-title-panel">
        <div class="workspace-panel__info-panel__name">
          <h3>{{ workspace.name }}</h3>
          <el-button
            v-if="!isCurrent"
            type="text"
            @click="switchToWorkspace"
          >
            Switch to this workspace
          </el-button>
        </div>
        <span class="sa-item-edit-link">
          <svgicon name="pencil-solid" />
          <el-button
            type="text"
            @click="navigateToWorkspaceEdit"
          >Edit</el-button>
        </span>
      </div>

      <div class="sa-item-attributes">
        <sa-attribute-value label="Default Currency">
          {{ workspace.defaultCurrency }}
        </sa-attribute-value>
        <br>
        <sa-attribute-value label="Workspace Shares">
          <el-table
            v-if="hasAccessTokens"
            :data="accessTokens"
          >
            <el-table-column
              label="Valid Till"
            >
              <template slot-scope="scope">
                {{ mediumDateTimeFormatterFromString(scope.row.validTill) }}
              </template>
            </el-table-column>
            <el-table-column align="right">
              <template slot-scope="scope">
                <div class="workspace-panel__share-link-panel">
                  <svgicon name="copy" />
                  <el-button
                    type="text"
                    @click="copyShareLink(scope.row.token)"
                  >
                    Copy link
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="workspace-panel__create-share-panel">
            {{ hasAccessTokens ? 'Add another share valid till' : 'Start sharing workspace, new link valid till' }}:
            <el-date-picker
              v-model="newShareValidTill"
              type="datetime"
              placeholder="Link valid till"
            />
            <svgicon name="share" />
            <el-button
              type="text"
              @click="shareWorkspace"
            >
              Create share link
            </el-button>
          </div>
        </sa-attribute-value>
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
