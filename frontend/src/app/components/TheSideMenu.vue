<template>
  <div class="the-side-menu">
    <the-side-menu-link to="/" title="Dashboard" icon="dashboard"/>
    <the-side-menu-link to="/expenses" title="Expenses" icon="expense"/>
    <the-side-menu-link to="/incomes" title="Incomes" icon="income"/>
    <the-side-menu-link to="/invoices" title="Invoices" icon="invoice"/>
    <the-side-menu-link to="/tax-payments" title="Tax payments" icon="tax"/>

    <span class="the-side-menu__category">Settings</span>

    <the-side-menu-link to="/settings/customers" title="Customers" icon="customer"/>
    <the-side-menu-link to="/settings/categories" title="Categories" icon="category"/>

    <span class="the-side-menu__category">User</span>

    <the-side-menu-link to="/my-profile" title="My Profile" icon="profile"/>
    <the-side-menu-link to="/logout" title="Logout" icon="logout"/>
  </div>
</template>

<script>
  import TheSideMenuLink from '@/app/components/TheSideMenuLink'

  export default {
    name: 'TheSideMenu',

    components: {
      TheSideMenuLink
    }
  }
</script>


<style lang="scss">
  @import "@/app/styles/vars.scss";
  @import "@/app/styles/mixins.scss";

  .the-side-menu {
  }

  .the-side-menu__link {
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

  .the-side-menu__category {
    display: block;
    margin: 10px 0;
    padding: 10px;
    text-transform: uppercase;
    font-weight: bold;
    font-size: 80%;
    opacity: 0.7;
  }
</style>