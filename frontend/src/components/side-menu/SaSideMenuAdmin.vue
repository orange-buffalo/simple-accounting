<template>
  <SaSideMenuLink
    to="/admin/users"
    :title="$t.navigationMenu.admin.users()"
    icon="dashboard"
  />
  <SaSideMenuLink
    to="/admin/oauth-providers"
    :title="$t.navigationMenu.admin.oauthProviders()"
    icon="authentication-providers"
  />
</template>

<script lang="ts" setup>
  import SaSideMenuLink from '@/components/side-menu/SaSideMenuLink.vue';
  import { $t } from '@/services/i18n';
</script>
