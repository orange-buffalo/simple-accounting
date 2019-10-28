<template>
  <div class="sa-action-link">
    <SaIcon
      :icon="icon"
      class="sa-action-link__icon"
    />
    <ElButton
      type="text"
      @click="click"
    >
      <slot />
    </ElButton>
  </div>
</template>

<script>
import SaIcon from '@/components/SaIcon';

export default {
  name: 'SaActionLink',

  components: {
    SaIcon,
  },

  props: {
    icon: {
      type: String,
      required: true,
    },
  },

  methods: {
    click() {
      this.$emit('click');
    },
  },
};
</script>


<style lang="scss">
  @import "@/styles/vars.scss";

  .sa-action-link {
    display: inline-flex;
    align-items: center;
    color: $components-color;

    .sa-action-link__icon {
      margin-right: 3px;
      height: 13px;
      width: 13px;
    }
  }

</style>
