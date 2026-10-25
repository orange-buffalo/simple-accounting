<template>
  <ElAside class="side-menu">
    <MenuLogo class="side-menu__logo" />

    <SaSideMenuUser v-if="isUser" />
    <SaSideMenuAdmin v-else />

    <span class="side-menu__category">{{ $t.navigationMenu.user.header() }}</span>

    <SaSideMenuLink
      v-if="isCurrentUserRegular()"
      to="/my-profile"
      :title="$t.navigationMenu.user.profile()"
      icon="profile"
    />
    <SaSideMenuLogoutButton />
  </ElAside>
</template>

<script lang="ts" setup>
  /// <reference types="vite-svg-loader" />
  import SaSideMenuLink from '@/components/side-menu/SaSideMenuLink.vue';
  import SaSideMenuLogoutButton from '@/components/side-menu/SaSideMenuLogoutButton.vue';
  import MenuLogo from '@/assets/logo-menu.svg?component';
  import { useAuth } from '@/services/api';
  import { $t } from '@/services/i18n';
  import SaSideMenuUser from '@/components/side-menu/SaSideMenuUser.vue';
  import SaSideMenuAdmin from '@/components/side-menu/SaSideMenuAdmin.vue';

  const {
    isAdmin,
    isCurrentUserRegular,
  } = useAuth();

  const isUser = !isAdmin();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss"as *;

  $menu-breakpoint: lg;

  .side-menu {
    background: $primary-color;
    color: $white;
    width: 70px !important;
    align-self: stretch;
    padding: 16px 8px;
    overflow-y: auto;
    flex-shrink: 0;

    @include respond-above($menu-breakpoint) {
      width: 252px !important;
      padding: 20px 14px;
    }

    &__logo {
      height: 54px;
      margin: 0 0 16px;
      display: inline-block;
      width: 100%;
      color: $white;

      @include respond-above($menu-breakpoint) {
        height: 68px;
      }
    }

    &__workspace-name {
      text-align: center;
      margin-bottom: 20px;
      color: #c2d1df;
      font-size: 13px;
      overflow-wrap: anywhere;
      display: none;

      @include respond-above($menu-breakpoint) {
        display: block;
      }
    }

    &__link {
      display: flex;
      padding: 10px 0;
      text-decoration: none;
      color: inherit;
      align-items: center;
      border-radius: 8px;
      margin-bottom: 3px;
      transition: all 0.1s ease-out;
      font-size: 0;
      cursor: pointer;

      @include respond-above($menu-breakpoint) {
        font-size: 14px;
        padding: 10px 12px;
      }

      &:hover {
        background-color: rgba(255, 255, 255, 0.08);
      }

      &.side-menu__link--active {
        background: $secondary-color;
        color: $white;
      }

      &:focus-visible {
        outline: 2px solid $accent-contrast-color;
        outline-offset: -2px;
      }

      .sa-icon {
        margin-right: 0;
        margin-left: 13px;
        flex-shrink: 0;
        height: 22px !important;
        width: 22px !important;

        @include respond-above($menu-breakpoint) {
          margin-right: 10px;
          margin-left: 0;
        }
      }
    }

    &__category {
      display: block;
      margin: 18px 0 8px;
      text-transform: uppercase;
      font-weight: bold;
      color: #a9bdce;
      font-size: 0;

      &:after {
        content: "";
        display: block;
        border-bottom: $white solid 1px;
      }

      @include respond-above($menu-breakpoint) {
        font-size: 10px;
        letter-spacing: 1.5px;
        padding: 0 12px;

        &:after {
          display: none;
        }
      }
    }
  }
</style>
