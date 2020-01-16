<template>
  <div class="sa-loading-progress">
    <div
      v-if="loading"
      class="sa-loading-progress__container"
    >
      <div
        class="sa-loading-progress__bar"
      />
    </div>
  </div>
</template>

<script>
  import { onMounted, onUnmounted, ref } from '@vue/composition-api';
  import { NAVIGATION_FINISHED_EVENT, NAVIGATION_STARTED_EVENT } from '@/services/events';

  export default {
    setup() {
      const loading = ref(false);

      const onLoadingStart = () => {
        loading.value = true;
      };

      const onLoadingFinished = () => {
        loading.value = false;
      };

      onMounted(() => {
        NAVIGATION_STARTED_EVENT.subscribe(onLoadingStart);
        NAVIGATION_FINISHED_EVENT.subscribe(onLoadingFinished);
      });

      onUnmounted(() => {
        NAVIGATION_STARTED_EVENT.unsubscribe(onLoadingStart);
        NAVIGATION_FINISHED_EVENT.unsubscribe(onLoadingFinished);
      });

      return {
        loading,
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";

  .sa-loading-progress {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    height: 3px;
    background-clip: padding-box;
    overflow: hidden;

    &__container {
      background-color: $primary-grey;
      z-index: 1000;
    }

    &__bar {
      background-color: $accent-primary-color;

      &:before {
        content: '';
        position: absolute;
        background-color: inherit;
        top: 0;
        left: 0;
        bottom: 0;
        will-change: left, right;
        animation: sa-loading-progress-bar 2.1s cubic-bezier(0.65, 0.815, 0.735, 0.395) infinite;
      }

      &:after {
        content: '';
        position: absolute;
        background-color: inherit;
        top: 0;
        left: 0;
        bottom: 0;
        will-change: left, right;
        animation: sa-loading-progress-bar-short 2.1s cubic-bezier(0.165, 0.84, 0.44, 1) infinite;
        animation-delay: 1.15s;
      }
    }
  }

  @keyframes sa-loading-progress-bar {
    0% {
      left: -35%;
      right: 100%;
    }
    60% {
      left: 100%;
      right: -90%;
    }
    100% {
      left: 100%;
      right: -90%;
    }
  }

  @keyframes sa-loading-progress-bar-short {
    0% {
      left: -200%;
      right: 100%;
    }
    60% {
      left: 107%;
      right: -8%;
    }
    100% {
      left: 107%;
      right: -8%;
    }
  }
</style>
