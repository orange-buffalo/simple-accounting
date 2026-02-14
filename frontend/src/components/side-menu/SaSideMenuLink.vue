<template>
  <RouterLink
    :to="to"
    :class="styleClass"
  >
    <SaIcon :icon="icon" :size="27" />
    {{ title }}
  </RouterLink>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import SaIcon from '@/components/SaIcon.vue';

const router = useRouter();

const props = defineProps<{
  to: string;
  title: string;
  icon: string;
}>();

const styleClass = computed(() => ({
  'side-menu__link': true,
  'side-menu__link--active': router.currentRoute.value.path === props.to,
}));
</script>
