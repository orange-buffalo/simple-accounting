<template>
  <ElTooltip :content="tooltip"
             :disabled="!tooltip"
             placement="bottom">
    <span class="overview-item-primary-attribute">
      <SaIcon :icon="icon" class="overview-item-primary-attribute__icon"/>
      <slot/>
    </span>
  </ElTooltip>
</template>

<script>
  import SaIcon from '@/components/SaIcon'

  export default {
    name: 'OverviewItemPrimaryAttribute',

    components: {
      SaIcon
    },

    props: {
      icon: {
        type: String,
        required: true
      },
      tooltip: {
        type: String
      }
    }
  }
</script>

<style lang="scss">
  .overview-item-primary-attribute {
    display: inline-flex;
    align-items: center;
    margin-right: 20px;

    &__icon {
      margin-right: 5px;
      width: 18px;
      height: 18px;
    }
  }
</style>