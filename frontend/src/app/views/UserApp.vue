<template>
  <el-container>
    <el-aside>
      <el-menu
          :router="true"
          background-color="#0a003a"
          text-color="#969c99"
          active-text-color="#ffffff">
        <el-menu-item index="/">
          <template slot="title">
            <i class="el-icon-location"></i>
            <span>Dashboard</span>
          </template>
        </el-menu-item>
        <el-menu-item index="/expenses">
          <template slot="title">
            <i class="el-icon-location"></i>
            <span>Expenses</span>
          </template>
        </el-menu-item>
        <el-submenu index="settings">
          <template slot="title">
            <i class="el-icon-location"></i>
            <span>Settings</span>
          </template>
          <el-menu-item index="/settings/categories">Categories</el-menu-item>
        </el-submenu>
      </el-menu>

    </el-aside>
    <el-container>
      <el-header>Header</el-header>
      <el-main>
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style lang="scss">
  .el-container {
    height: 100vh;
  }

  .el-aside {
    background-color: #0a003a;
  }

  .el-main {
    background-color: rgb(249, 251, 253);
  }

  .el-menu {
    border-right: 0 !important;
  }
</style>

<script>
  export default {
    name: 'UserApp'
  }
</script>
