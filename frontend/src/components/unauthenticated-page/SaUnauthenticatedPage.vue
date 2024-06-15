<template>
  <ElContainer direction="vertical" class="sa-unauthenticated-page__container">
    <LogoLogin class="sa-unauthenticated-page__logo" />
    <div class="sa-unauthenticated-page__content">
      <RouterView />
    </div>
  </ElContainer>
</template>

<script lang="ts" setup>
  import LogoLogin from '@/assets/logo-login.svg?component';
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-unauthenticated-page {
    &__container {
      min-height: 100vh;
      height: auto;
      position: relative;
      align-items: center;
      background-color: $primary-grey;
    }

    &__logo {
      min-height: 150px;
      height: 150px;
      min-width: 150px;
      width: 150px;
      margin-bottom: 40px;
      margin-top: 60px;
    }

    &__content {
      max-width: 700px;
      padding-bottom: 20px;
    }
  }
</style>
