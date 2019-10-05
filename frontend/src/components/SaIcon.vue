<template>
  <svgicon :name="icon"/>
</template>

<script>
  export default {
    name: 'SaIcon',

    props: {
      icon: {
        type: String,
        required: true
      }
    },

    created: function () {
      require(`./icons/${this.icon}`);
    }
  }
</script>