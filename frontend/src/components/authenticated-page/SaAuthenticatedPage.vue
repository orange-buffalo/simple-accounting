<template>
  <ElContainer>
    <SaSideMenu />

    <ElContainer>
      <SaAuthenticatedPageLoadingProgress />
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
import SaAuthenticatedPageLoadingProgress from '@/components/authenticated-page/SaAuthenticatedPageLoadingProgress.vue';
import SaSideMenu from '@/components/side-menu/SaSideMenu.vue';
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
