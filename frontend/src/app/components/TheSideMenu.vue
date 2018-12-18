<template>
  <div class="the-side-menu">
    <!--todo icons-->
    <the-side-menu-link to="/" title="Dashboard" icon="pencil"/>
    <the-side-menu-link to="/expenses" title="Expenses"/>
    <the-side-menu-link to="/incomes" title="Incomes"/>
    <the-side-menu-link to="/invoices" title="Invoices"/>
    <the-side-menu-link to="/tax-payments" title="Tax payments"/>

    <span class="the-side-menu__category">Settings</span>

    <the-side-menu-link to="/settings/customers" title="Customers"/>
    <the-side-menu-link to="/settings/categories" title="Categories"/>

    <span class="the-side-menu__category">User</span>

    <the-side-menu-link to="/my-profile" title="My Profile"/>
    <the-side-menu-link to="/logout" title="Logout"/>
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
  .the-side-menu {
  }

  .the-side-menu__link {
    display: block;
    padding: 10px;
    text-decoration: none;
    color: inherit;

    &:hover {
      background-color: #4b677c;
    }

    &.the-side-menu__link--active {
      border-left: 2px solid red;
    }
  }

  .the-side-menu__category {
    display: block;
    margin: 10px 0;
    padding: 10px;
    text-transform: uppercase;
    font-weight: bold;
    font-size: 80%;
  }
</style>