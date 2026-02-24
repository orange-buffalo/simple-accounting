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
  import { useMutation } from '@/services/api/use-gql-api.ts';
  import { graphql } from '@/services/api/gql';

  const router = useRouter();

  const { logout } = useAuth();

  const invalidateRefreshToken = useMutation(
    graphql(/* GraphQL */ `
      mutation invalidateRefreshToken {
        invalidateRefreshToken
      }
    `),
    'invalidateRefreshToken',
  );

  const doLogout = async () => {
    try {
      await invalidateRefreshToken({});
    } finally {
      logout();
      await router.push('/login');
    }
  };
</script>
