<template>
  <ElContainer>
    <TheSideMenu />

    <ElContainer>
      <TheLoadingProgress />
      <ElMain>
        <div class="content-panel">
          <Transition
            name="authenticated-page-transition"
            mode="out-in"
          >
            <RouterView />
          </Transition>
        </div>
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<script>
  import TheSideMenu from '@/components/TheSideMenu';
  import TheLoadingProgress from '@/components/loaders/TheLoadingProgress';

  export default {
    name: 'TheAuthenticatedPage',

    components: {
      TheSideMenu,
      TheLoadingProgress,
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

  .authenticated-page-transition {
    &-enter, &-leave-to {
      opacity: 0;
    }

    &-enter-active, &-leave-active {
      transition: opacity 0.1s;
    }
  }
</style>
