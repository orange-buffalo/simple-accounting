<template>
  <SvgIcon
    :name="loadedIcon"
    @click="$emit('click')"
  />
</template>

<script>
  // eslint-disable-next-line import/no-extraneous-dependencies
  import SvgIcon from 'vue-svgicon';

  export default {
    name: 'SaIcon',

    components: {
      SvgIcon,
    },

    props: {
      icon: {
        type: String,
        required: true,
      },
    },

    data() {
      return {
        // supports dynamic change of an icon with necessary data loading
        loadedIcon: null,
      };
    },

    watch: {
      icon() {
        this.loadIcon();
      },
    },

    created() {
      this.loadIcon();
    },

    methods: {
      async loadIcon() {
        await import(`./icons/${this.icon}`);
        this.loadedIcon = this.icon;
      },
    },
  };
</script>

<style lang="scss">
  .svg-icon {
    display: inline-block;
    width: 16px;
    height: 16px;
    color: inherit;
    vertical-align: middle;
    fill: none;
    stroke: currentColor;
  }

  .svg-fill {
    fill: currentColor;
    stroke: none;
  }
</style>
