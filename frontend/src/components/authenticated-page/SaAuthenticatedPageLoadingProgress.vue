<template>
  <div class="loading-progress">
    <div
      v-if="loading"
      class="loading-progress__container"
    >
      <div class="loading-progress__bar" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { throttle } from 'lodash';
import { onMounted, onUnmounted, ref } from 'vue';
import { LOADING_FINISHED_EVENT, LOADING_STARTED_EVENT } from '@/services/events';

const loading = ref(false);
let loadingRequestsCount = 0;

const toggleProgress = throttle(
  () => {
    loading.value = loadingRequestsCount > 0;
  },
  400,
  {
    trailing: true,
  },
);

const onLoadingStart = () => {
  loadingRequestsCount += 1;
  toggleProgress();
};

const onLoadingFinished = () => {
  loadingRequestsCount = Math.max(loadingRequestsCount - 1, 0);
  toggleProgress();
};

onMounted(() => {
  LOADING_STARTED_EVENT.subscribe(onLoadingStart);
  LOADING_FINISHED_EVENT.subscribe(onLoadingFinished);
});

onUnmounted(() => {
  LOADING_STARTED_EVENT.unsubscribe(onLoadingStart);
  LOADING_FINISHED_EVENT.unsubscribe(onLoadingFinished);
});
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .loading-progress {
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
        animation: loading-progress-bar 2.1s cubic-bezier(0.65, 0.815, 0.735, 0.395) infinite;
      }

      &:after {
        content: '';
        position: absolute;
        background-color: inherit;
        top: 0;
        left: 0;
        bottom: 0;
        will-change: left, right;
        animation: loading-progress-bar-short 2.1s cubic-bezier(0.165, 0.84, 0.44, 1) infinite;
        animation-delay: 1.15s;
      }
    }
  }

  @keyframes loading-progress-bar {
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

  @keyframes loading-progress-bar-short {
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
