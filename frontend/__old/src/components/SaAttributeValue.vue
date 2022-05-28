<template>
  <div class="sa-attribute-value">
    <div class="sa-attribute-value__label">
      {{ label }}
    </div>
    <div>
      <slot />
    </div>
  </div>
</template>

<script>

  export default {
    name: 'SaAttributeValue',

    props: {
      label: String,
    },
  };
</script>

<style lang="scss">
  .sa-attribute-value {

    &__label {
      font-weight: bold;
    }
  }

</style>
