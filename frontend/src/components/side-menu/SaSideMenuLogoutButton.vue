<template>
  <button type="button" class="side-menu__link side-menu__logout" :title="$t.navigationMenu.user.logout()" @click="doLogout">
    <SaIcon icon="logout" :size="27" variant="detailed" />
    {{
      $t.navigationMenu.user.logout()
    }}
  </button>
</template>

<script setup lang="ts">
  import { ROUTE_NAMES } from '@/services/route-names';
  import { useRouter } from 'vue-router';
  import { $t } from '@/services/i18n';
  import SaIcon from '@/components/SaIcon.vue';
  import { useAuth } from '@/services/api';

  const router = useRouter();

  const { logout } = useAuth();
  const doLogout = async () => {
    await logout();
    await router.push({ name: ROUTE_NAMES.login });
  };
</script>

<style scoped>
  .side-menu__logout {
    width: 100%;
    border: 0;
    background: transparent;
    font-family: inherit;
    text-align: left;
  }
</style>
