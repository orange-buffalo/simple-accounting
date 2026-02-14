<template>
  <SaSideMenuLink
    to="/admin/users"
    :title="$t.navigationMenu.admin.users()"
    icon="dashboard"
  />
</template>

<script lang="ts" setup>
import SaSideMenuLink from '@/components/side-menu/SaSideMenuLink.vue';
import { $t } from '@/services/i18n';
</script>
