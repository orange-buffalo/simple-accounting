<template>
  <div class="workspace-panel">
    <div class="workspace-panel__info-panel">
      <div class="sa-item-title-panel">
        <div class="workspace-panel__info-panel__name">
          <h3>{{workspace.name}}</h3>
          <el-button type="text"
                     v-if="!isCurrent"
                     @click="switchToWorkspace">Switch to this workspace
          </el-button>
        </div>
        <span class="sa-item-edit-link">
          <svgicon name="pencil"/>
          <el-button type="text"
                     @click="navigateToWorkspaceEdit">Edit</el-button>
        </span>
      </div>

      <div class="sa-item-attributes">
        <sa-attribute-value label="Default Currency">
          {{ workspace.defaultCurrency}}
        </sa-attribute-value>
        <br/>
        <sa-attribute-value label="Workspace Shares">
          <el-table v-if="hasAccessTokens"
                    :data="accessTokens">
            <el-table-column
                label="Valid Till">
              <template slot-scope="scope">
                {{mediumDateTimeFormatterFromString(scope.row.validTill)}}
              </template>
            </el-table-column>
            <el-table-column align="right">
              <template slot-scope="scope">
                <div class="workspace-panel__share-link-panel">
                  <svgicon name="copy"/>
                  <el-button type="text"
                             @click="copyShareLink(scope.row.token)">
                    Copy link
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="workspace-panel__create-share-panel">
            {{hasAccessTokens ? 'Add another share valid till' : 'Start sharing workspace, new link valid till'}}:
            <el-date-picker
                v-model="newShareValidTill"
                type="datetime"
                placeholder="Link valid till">
            </el-date-picker>
            <svgicon name="share"/>
            <el-button type="text"
                       @click="shareWorkspace">
              Create share link
            </el-button>
          </div>
        </sa-attribute-value>
      </div>
    </div>
  </div>
</template>

<script>
  import '@/components/icons/pencil'
  import '@/components/icons/share'
  import '@/components/icons/copy'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import SaAttributeValue from '@/components/SaAttributeValue'
  import {api} from '@/services/api'
  import {withMediumDateTimeFormatter} from '@/components/mixins/with-medium-datetime-formatter'
  import copy from 'copy-to-clipboard';

  export default {
    name: 'TheWorkspacesOverviewItemPanel',

    mixins: [withWorkspaces, withMediumDateTimeFormatter],

    components: {
      SaAttributeValue
    },

    props: {
      workspace: Object
    },

    data: function () {
      return {
        accessTokens: [],
        newShareValidTill: new Date()
      }
    },

    created: async function () {
      this._reloadAccessTokens()
    },

    computed: {
      hasAccessTokens: function () {
        return this.accessTokens.length
      },

      isCurrent: function () {
        return this.workspace.id === this.currentWorkspace.id
      }
    },

    methods: {
      navigateToWorkspaceEdit: function () {
        this.$router.push({name: 'edit-workspace', params: {id: this.workspace.id}})
      },

      switchToWorkspace: function () {
        // todo #90: do not commit directly, use wrapper action
        this.$store.commit('workspaces/setCurrentWorkspace', this.workspace)
        this.$router.push("/")
      },

      shareWorkspace: async function () {
        await api.post(`/workspaces/${this.workspace.id}/workspace-access-tokens`, {
          validTill: this.newShareValidTill.toISOString()
        })
        this._reloadAccessTokens()
      },

      _reloadAccessTokens: async function () {
        let response = await api.get(`/workspaces/${this.workspace.id}/workspace-access-tokens`)
        this.accessTokens = response.data.data
      },

      copyShareLink: function (token) {
        let shareLink = `${window.location.origin}/login-by-link/${token}`
        copy(shareLink)
      }
    }
  }
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
