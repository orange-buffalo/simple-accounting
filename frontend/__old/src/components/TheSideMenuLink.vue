<template>
  <RouterLink
    :to="to"
    :class="styleClass"
  >
    <SaIcon
      v-if="icon"
      :icon="icon"
    />
    {{ title }}
  </RouterLink>
</template>

<script>
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'TheSideMenuLink',

    components: { SaIcon },

    props: {
      to: {
        type: String,
        required: true,
      },
      title: {
        type: String,
        required: true,
      },
      icon: {
        type: String,
        required: true,
      },
    },

    computed: {
      styleClass() {
        return {
          'the-side-menu__link': true,
          'the-side-menu__link--active': this.$route.path === this.to,
        };
      },
    },
  };
</script>
