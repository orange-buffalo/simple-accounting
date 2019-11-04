<template>
  <Svgicon :name="icon" />
</template>

<script>
  export default {
    name: 'SaIcon',

    props: {
      icon: {
        type: String,
        required: true,
      },
    },

    created() {
      require(`./icons/${this.icon}`);
    },
  };
</script>
