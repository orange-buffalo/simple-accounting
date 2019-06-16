<template>
  <router-link :to="to"
               :class="styleClass">
    <svgicon v-if="icon" :name="icon"/>
    {{title}}
  </router-link>
</template>

<script>
  export default {
    name: 'TheSideMenuLink',

    props: {
      to: String,
      title: String,
      icon: String
    },

    created: function () {
      if (this.icon) {
        import(`@/components/icons/${this.icon}`)
      }
    },

    computed: {
      styleClass: function () {
        return {
          'the-side-menu__link': true,
          'the-side-menu__link--active': this.$route.path === this.to
        }
      }
    }
  }
</script>