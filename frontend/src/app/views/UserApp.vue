<template>
  <el-container>
    <el-aside>
      <div class="logo"></div>

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

    .logo {
      height: 120px;
      background: url("../../assets/logo-menu.svg");
      background-size: contain;
      background-repeat: no-repeat;
      background-position: center;
      margin: 10px 0 10px 0;
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
