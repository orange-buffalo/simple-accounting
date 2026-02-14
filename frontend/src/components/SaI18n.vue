<template>
  <Component :is="componentImpl" />
</template>

<script lang="ts" setup>
import type { Component } from 'vue';
import { computed, h, useSlots } from 'vue';
import { parseMessage } from '@/services/i18n';

const props = defineProps<{
  message: string;
}>();

const slots = useSlots();

const componentImpl = computed<Component>(() => {
  const children = parseMessage(props.message).flatMap((parsedToken) => {
    if (parsedToken.type === 'content') {
      return h('span', parsedToken.value);
    }
    const slot = slots[parsedToken.value];
    if (!slot) {
      throw new Error(`Cannot find slot ${parsedToken.value}`);
    }
    return slot();
  });
  return () => children;
});
</script>
