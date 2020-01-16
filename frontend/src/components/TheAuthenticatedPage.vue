<template>
  <ElContainer>
    <TheSideMenu />

    <ElContainer>
      <SaLoadingProgress />
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
  import SaLoadingProgress from '@/components/SaLoadingProgress';

  export default {
    name: 'TheAuthenticatedPage',

    components: {
      TheSideMenu,
      SaLoadingProgress,
    },
  };
</script>


<style lang="scss">
  @import "~@/styles/vars.scss";

  .el-container {
    height: 100vh;
    position: relative;
  }

  .el-main {
    background-color: $primary-grey;

    .content-panel {
      max-width: 1500px;
      margin: auto;
    }
  }
</style>
