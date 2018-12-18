<template>
  <el-container>
    <el-aside>
      <div class="avatar">
      </div>
      <div class="workspace">
        {{currentWorkspace.name}}
        <el-popover
            placement="bottom"
            width="200"
            trigger="click">
          <span slot="reference">
            <svgicon name="gear"/>
          </span>
          Test
          <el-button>test button</el-button>
          <hr/>

          <div v-if="otherWorkspaces.length">
            <span>Switch to another workspace</span>
            <div v-for="workspace in otherWorkspaces">
              {{workspace.id}}: {{workspace.name}}
            </div>
            <hr/>
          </div>

        </el-popover>

      </div>

      <the-side-menu/>

    </el-aside>
    <el-container>
      <el-main>
        <div class="content-panel">
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
  import {withWorkspace} from '@/app/components/mixins/with-workspace'
  import '@/components/icons/gear'
  import TheSideMenu from '@/app/components/TheSideMenu'

  export default {
    name: 'UserApp',

    mixins: [withWorkspace],

    components: {TheSideMenu},

    computed: {
      otherWorkspaces: function () {
        return this.workspaces.filter(it => it.id !== this.currentWorkspace.id)
      }
    }
  }
</script>


<style lang="scss">
  .el-container {
    height: 100vh;
  }

  .el-aside {
    background-color: #3D5265;
    color: white;

    .avatar {
      border-radius: 50%;
      width: 100px;
      height: 100px;
      margin: 20px auto 10px;
      background-image: url("https://www.atomix.com.au/media/2015/06/atomix_user31.png");
      background-size: cover;
    }

    .workspace {
      text-align: center;
      margin-bottom: 20px;
    }
  }

  .el-main {
    background-color: rgb(249, 251, 253);

    .content-panel {
      max-width: 1500px;
      margin: auto;
    }
  }

  .el-menu {
    border-right: 0 !important;
  }
</style>
