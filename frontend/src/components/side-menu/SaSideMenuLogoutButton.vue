<template>
  <div class="side-menu__link" @click="doLogout">
    <SaIcon icon="logout" :size="27" />
    {{
      $t.navigationMenu.user.logout()
    }}
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { $t } from '@/services/i18n';
  import SaIcon from '@/components/SaIcon.vue';
  import { useAuth } from '@/services/api';

  const router = useRouter();

  const { logout } = useAuth();
  const doLogout = async () => {
    await logout();
    await router.push('/login');
  };
</script>
