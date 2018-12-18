<template>
  <el-container>
    <el-aside>
      <div class="avatar">
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

  export default {
    name: 'UserApp',

    components: {TheSideMenu, TheWorkspaceSelector}
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
