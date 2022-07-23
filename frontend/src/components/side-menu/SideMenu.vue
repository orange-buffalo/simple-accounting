<template>
  <ElAside class="side-menu">
    <MenuLogo class="side-menu__logo" />

    <div
        v-if="isUser"
        class="side-menu__workspace-name"
    >
      {{ currentWorkspace.name }}
    </div>

    <template v-if="isUser">
      <TheSideMenuLink
          to="/"
          :title="$t.navigationMenu.dashboard()"
          icon="dashboard"
      />
      <TheSideMenuLink
          to="/expenses"
          :title="$t.navigationMenu.expenses()"
          icon="expense"
      />
      <TheSideMenuLink
          to="/incomes"
          :title="$t.navigationMenu.incomes()"
          icon="income"
      />
      <TheSideMenuLink
          to="/invoices"
          :title="$t.navigationMenu.invoices()"
          icon="invoices-overview"
      />
      <TheSideMenuLink
          to="/income-tax-payments"
          :title="$t.navigationMenu.taxPayments()"
          icon="income-tax-payments-overview"
      />
      <TheSideMenuLink
          to="/reporting"
          :title="$t.navigationMenu.reporting()"
          icon="reporting"
      />

      <template v-if="isCurrentUserRegular()">
        <span class="side-menu__category">{{ $t.navigationMenu.settings.header() }}</span>

        <TheSideMenuLink
            v-if="currentWorkspace.editable"
            to="/settings/customers"
            :title="$t.navigationMenu.settings.customers()"
            icon="customers-overview"
        />

        <TheSideMenuLink
            v-if="currentWorkspace.editable"
            to="/settings/categories"
            :title="$t.navigationMenu.settings.categories()"
            icon="category"
        />

        <TheSideMenuLink
            v-if="currentWorkspace.editable"
            to="/settings/general-taxes"
            :title="$t.navigationMenu.settings.generalTaxes()"
            icon="taxes-overview"
        />

        <TheSideMenuLink
            to="/settings/workspaces"
            :title="$t.navigationMenu.settings.workspaces()"
            icon="workspaces"
        />
      </template>
    </template>

    <span class="side-menu__category">{{ $t.navigationMenu.user.header() }}</span>

    <TheSideMenuLink
        v-if="isCurrentUserRegular()"
        to="/my-profile"
        :title="$t.navigationMenu.user.profile()"
        icon="profile"
    />
    <TheSideMenuLink
        to="/logout"
        :title="$t.navigationMenu.user.logout()"
        icon="logout"
    />
  </ElAside>
</template>

<script lang="ts" setup>
  /// <reference types="vite-svg-loader" />
  import { onUnmounted, ref } from 'vue';
  import TheSideMenuLink from '@/components/side-menu/SideMenuLink.vue';
  import MenuLogo from '@/assets/logo-menu.svg?component';
  import { useAuth } from '@/services/api';
  import type { WorkspaceDto } from '@/services/api';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { WORKSPACE_CHANGED_EVENT } from '@/services/events';
  import { $t } from '@/services/i18n';

  const {
    isAdmin,
    isCurrentUserRegular,
  } = useAuth();

  const currentWorkspace = ref(useCurrentWorkspace().currentWorkspace);
  const onWorkspaceChange = (newWorkspace: WorkspaceDto) => {
    currentWorkspace.value = newWorkspace;
  };
  WORKSPACE_CHANGED_EVENT.subscribe(onWorkspaceChange);
  onUnmounted(() => WORKSPACE_CHANGED_EVENT.unsubscribe(onWorkspaceChange));

  const isUser = !isAdmin();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss"as *;

  $menu-breakpoint: lg;

  .side-menu {
    @include gradient-background;
    color: $white;
    width: 70px !important;

    @include respond-above($menu-breakpoint) {
      width: 300px !important;
    }

    &__logo {
      height: 70px;
      margin: 10px 0 10px 0;
      display: inline-block;
      width: 100%;
      color: $accent-contrast-color;

      @include respond-above($menu-breakpoint) {
        height: 120px;
      }
    }

    &__workspace-name {
      text-align: center;
      margin-bottom: 20px;
      color: $accent-contrast-color;
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
      border-left: 3px solid transparent;
      transition: all 0.1s ease-out;
      font-size: 0;

      @include respond-above($menu-breakpoint) {
        border-left: 5px solid transparent;
        font-size: inherit;
        padding: 10px;
      }

      &:hover {
        background-color: $accent-primary-color;
      }

      &.side-menu__link--active {
        border-left: 3px solid $accent-contrast-color;

        @include respond-above($menu-breakpoint) {
          border-left: 5px solid $accent-contrast-color;
        }
      }

      .sa-icon {
        margin-right: 0;
        margin-left: 17px;

        @include respond-above($menu-breakpoint) {
          margin-right: 10px;
          margin-left: 0;
          height: 25px;
          width: 25px;
        }
      }
    }

    &__category {
      display: block;
      margin: 10px 0;
      text-transform: uppercase;
      font-weight: bold;
      opacity: 0.2;
      font-size: 0;

      &:after {
        content: "";
        display: block;
        border-bottom: $white solid 1px;
      }

      @include respond-above($menu-breakpoint) {
        font-size: 80%;
        padding: 10px;
        opacity: 0.7;

        &:after {
          display: none;
        }
      }
    }
  }
</style>
