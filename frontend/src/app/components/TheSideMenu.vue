<template>
  <el-aside class="the-side-menu">
    <div class="the-side-menu__logo"></div>

    <div class="the-side-menu__workspace-name">
      {{currentWorkspace.name}}
    </div>

    <the-side-menu-link to="/" title="Dashboard" icon="dashboard"/>
    <the-side-menu-link to="/expenses" title="Expenses" icon="expense"/>
    <the-side-menu-link to="/incomes" title="Incomes" icon="income"/>
    <the-side-menu-link to="/invoices" title="Invoices" icon="invoice"/>
    <the-side-menu-link to="/tax-payments" title="Tax payments" icon="tax"/>
    <the-side-menu-link to="/reporting" title="Reporting" icon="reporting"/>

    <span class="the-side-menu__category">Settings</span>

    <the-side-menu-link to="/settings/customers" title="Customers" icon="customer"/>
    <the-side-menu-link to="/settings/categories" title="Categories" icon="category"/>
    <the-side-menu-link to="/settings/taxes" title="Taxes" icon="tax"/>
    <the-side-menu-link to="/settings/workspaces" title="Workspaces" icon="workspaces"/>

    <span class="the-side-menu__category">User</span>

    <the-side-menu-link to="/my-profile" title="My Profile" icon="profile"/>
    <the-side-menu-link to="/logout" title="Logout" icon="logout"/>
  </el-aside>
</template>

<script>
  import TheSideMenuLink from '@/app/components/TheSideMenuLink'
  import '@/components/icons/reporting'
  import '@/components/icons/dashboard'
  import '@/components/icons/expense'
  import '@/components/icons/income'
  import '@/components/icons/invoice'
  import '@/components/icons/tax'
  import '@/components/icons/customer'
  import '@/components/icons/category'
  import '@/components/icons/workspaces'
  import withWorkspaces from '@/app/components/mixins/with-workspaces'

  export default {
    name: 'TheSideMenu',

    mixins: [withWorkspaces],

    components: {
      TheSideMenuLink
    }
  }
</script>

<style lang="scss">
  @import "@/app/styles/vars.scss";
  @import "@/app/styles/mixins.scss";

  .the-side-menu {
    @include gradient-background;
    color: $white;
    width: 300px !important;

    &__logo {
      height: 120px;
      background: url("../../assets/logo-menu.svg");
      background-size: contain;
      background-repeat: no-repeat;
      background-position: center;
      margin: 10px 0 10px 0;
    }

    &__workspace-name {
      text-align: center;
      margin-bottom: 20px;
      color: $accent-contrast-color;
    }

    &__link {
      display: flex;
      padding: 10px;
      text-decoration: none;
      color: inherit;
      align-items: center;
      border-left: 5px solid transparent;
      transition: all 0.1s ease-out;

      &:hover {
        background-color: $accent-primary-color;
      }

      &.the-side-menu__link--active {
        border-left: 5px solid $accent-contrast-color;
      }

      .svg-icon {
        margin-right: 10px;
        height: 22px;
        width: 22px;
      }
    }

    &__category {
      display: block;
      margin: 10px 0;
      padding: 10px;
      text-transform: uppercase;
      font-weight: bold;
      font-size: 80%;
      opacity: 0.7;
    }
  }

</style>