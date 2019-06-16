<template>
  <el-container>
    <the-side-menu/>

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
  import TheSideMenu from '@/components/TheSideMenu'

  export default {
    name: 'UserApp',

    components: {TheSideMenu}
  }
</script>


<style lang="scss">
  @import "@/styles/vars.scss";

  .el-container {
    height: 100vh;
  }

  .el-main {
    background-color: $primary-grey;

    .content-panel {
      max-width: 1500px;
      margin: auto;
    }
  }
</style>
