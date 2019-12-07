<template>
  <ElAside class="the-side-menu">
    <div class="the-side-menu__logo" />

    <div
      v-if="isUser"
      class="the-side-menu__workspace-name"
    >
      {{ currentWorkspace.name }}
    </div>

    <template v-if="isUser">
      <TheSideMenuLink
        to="/"
        title="Dashboard"
        icon="dashboard"
      />
      <TheSideMenuLink
        to="/expenses"
        title="Expenses"
        icon="expense"
      />
      <TheSideMenuLink
        to="/incomes"
        title="Incomes"
        icon="income"
      />
      <TheSideMenuLink
        to="/invoices"
        title="Invoices"
        icon="invoices-overview"
      />
      <TheSideMenuLink
        to="/income-tax-payments"
        title="Income Tax Payments"
        icon="taxes-overview"
      />
      <TheSideMenuLink
        to="/reporting"
        title="Reporting"
        icon="reporting"
      />

      <template v-if="isCurrentUserRegular">
        <span class="the-side-menu__category">Settings</span>

        <TheSideMenuLink
          v-if="currentWorkspace.editable"
          to="/settings/customers"
          title="Customers"
          icon="customer"
        />

        <TheSideMenuLink
          v-if="currentWorkspace.editable"
          to="/settings/categories"
          title="Categories"
          icon="category"
        />

        <TheSideMenuLink
          v-if="currentWorkspace.editable"
          to="/settings/general-taxes"
          title="General Taxes"
          icon="taxes-overview"
        />

        <TheSideMenuLink
          to="/settings/workspaces"
          title="Workspaces"
          icon="workspaces"
        />
      </template>
    </template>

    <span class="the-side-menu__category">User</span>

    <TheSideMenuLink
      v-if="isCurrentUserRegular"
      to="/my-profile"
      title="My Profile"
      icon="profile"
    />
    <TheSideMenuLink
      to="/logout"
      title="Logout"
      icon="logout"
    />
  </ElAside>
</template>

<script>
  import { mapState } from 'vuex';
  import TheSideMenuLink from '@/components/TheSideMenuLink';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import { withApi } from '@/components/mixins/with-api';

  export default {
    name: 'TheSideMenu',

    components: {
      TheSideMenuLink,
    },

    mixins: [withWorkspaces, withApi],

    computed: {
      ...mapState({
        isUser: state => !state.api.isAdmin,
      }),
    },
  };
</script>

<style lang="scss">
  @import "@/styles/vars.scss";
  @import "@/styles/mixins.scss";

  $menu-breakpoint: lg;

  .the-side-menu {
    @include gradient-background;
    color: $white;
    width: 70px !important;

    @include respond-above($menu-breakpoint) {
      width: 300px !important;
    }

    &__logo {
      height: 70px;
      margin: 10px 0 10px 0;
      background: url("../assets/logo-menu.svg");
      background-size: contain;
      background-repeat: no-repeat;
      background-position: center;

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

      &.the-side-menu__link--active {
        border-left: 3px solid $accent-contrast-color;

        @include respond-above($menu-breakpoint) {
          border-left: 5px solid $accent-contrast-color;
        }
      }

      .svg-icon {
        margin-right: 0;
        height: 30px;
        width: 30px;
        margin-left: 17px;

        @include respond-above($menu-breakpoint) {
          margin-right: 10px;
          margin-left: 0;
          height: 22px;
          width: 22px;
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
