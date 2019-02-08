<template>
  <el-container>
    <el-aside>
      <div class="avatar">
        <svgicon name="user"/>
      </div>

      <the-workspace-selector/>

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
  import TheSideMenu from '@/app/components/TheSideMenu'
  import TheWorkspaceSelector from '@/app/components/TheWorkspaceSelector'
  import '@/components/icons/user.js'

  export default {
    name: 'UserApp',

    components: {TheSideMenu, TheWorkspaceSelector}
  }
</script>


<style lang="scss">
  @import "@/app/styles/vars.scss";
  @import "@/app/styles/mixins.scss";

  .el-container {
    height: 100vh;
  }

  .el-aside {
    @include gradient-background;
    color: $white;

    .avatar {
      text-align: center;
      .svg-icon {
        border-radius: 50%;
        width: 100px;
        height: 100px;
        margin: 20px auto 10px;
      }
    }
  }

  .el-main {
    background-color: $primary-grey;

    .content-panel {
      max-width: 1500px;
      margin: auto;
    }
  }

  .el-menu {
    border-right: 0 !important;
  }
</style>
