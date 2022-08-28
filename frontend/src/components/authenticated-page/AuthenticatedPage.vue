<template>
  <ElContainer>
    <SideMenu />

    <ElContainer>
      <AuthenticatedPageLoadingProgress />
      <ElMain>
        <div class="content-panel">
          <RouterView v-slot="{ Component: rv }">
            <Transition
              name="authenticated-page-transition"
              mode="out-in"
            >
              <Component :is="rv" />
            </Transition>
          </RouterView>
        </div>
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<script lang="ts" setup>
  import SideMenu from '@/components/side-menu/SideMenu.vue';
  import AuthenticatedPageLoadingProgress from '@/components/authenticated-page/AuthenticatedPageLoadingProgress.vue';

</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

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
