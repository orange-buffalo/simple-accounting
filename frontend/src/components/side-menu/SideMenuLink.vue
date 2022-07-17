<template>
  <RouterLink
      :to="to"
      :class="styleClass"
  >
    <SaIcon :icon="icon" />
    {{ title }}
  </RouterLink>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import SaIcon from '@/components/SaIcon.vue';
  import { app } from '@/services/app-services';

  const props = defineProps<{
    to: string,
    title: string,
    icon: string,
  }>();

  const styleClass = computed(() => ({
    'side-menu__link': true,
    'side-menu__link--active': app.router.currentRoute.value.path === props.to,
  }));
</script>
