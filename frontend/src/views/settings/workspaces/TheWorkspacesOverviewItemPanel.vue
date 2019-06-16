<template>
  <div class="workspace-panel">
    <div class="workspace-panel__info-panel">
      <div class="sa-item-title-panel">
        <div class="workspace-panel__info-panel__name">
          <h3>{{workspace.name}}</h3>
          <el-button type="text"
                     v-if="!isCurrent(workspace)"
                     @click="switchToWorkspace(workspace)">Switch to this workspace
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
      </div>
    </div>
  </div>
</template>

<script>
  import '@/components/icons/pencil'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import SaAttributeValue from '@/components/SaAttributeValue'

  export default {
    name: 'TheWorkspacesOverviewItemPanel',

    mixins: [withWorkspaces],

    components: {
      SaAttributeValue
    },

    props: {
      workspace: Object
    },

    methods: {
      navigateToWorkspaceEdit: function () {
        this.$router.push({name: 'edit-workspace', params: {id: this.workspace.id}})
      },

      switchToWorkspace: function (ws) {
        // todo #90: do not commit directly, use wrapper action
        this.$store.commit('workspaces/setCurrentWorkspace', ws)
        this.$router.push("/")
      },

      isCurrent: function (ws) {
        return ws.id === this.currentWorkspace.id
      }
    }
  }
</script>

<style lang="scss">
  @import "@/styles/main.scss";

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
  }
</style>
