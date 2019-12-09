<template>
  <ElContainer>
    <TheSideMenu />

    <ElContainer>
      <ElMain>
        <div class="content-panel">
          <RouterView />
        </div>
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<script>
  import TheSideMenu from '@/components/TheSideMenu';

  export default {
    name: 'UserApp',

    components: { TheSideMenu },
  };
</script>


<style lang="scss">
  @import "~@/styles/vars.scss";

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
