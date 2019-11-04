<template>
  <RouterLink
    :to="to"
    :class="styleClass"
  >
    <Svgicon
      v-if="icon"
      :name="icon"
    />
    {{ title }}
  </RouterLink>
</template>

<script>
  export default {
    name: 'TheSideMenuLink',

    props: {
      to: String,
      title: String,
      icon: String,
    },

    computed: {
      styleClass() {
        return {
          'the-side-menu__link': true,
          'the-side-menu__link--active': this.$route.path === this.to,
        };
      },
    },

    created() {
      if (this.icon) {
        import(`@/components/icons/${this.icon}`);
      }
    },
  };
</script>
