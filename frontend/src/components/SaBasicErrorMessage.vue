<template>
  <SaStatusLabel
    simplified
    status="failure"
    class="sa-basic-error-message"
  >
    {{ errorMessage }}
  </SaStatusLabel>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import SaStatusLabel from '@/components/SaStatusLabel.vue';
import { $t } from '@/services/i18n';

const props = defineProps<{ message?: string }>();

const errorMessage = computed(() => props.message || $t.value.saBasicErrorMessage.defaultMessage());
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss"as *;

  .sa-basic-error-message {
    white-space: nowrap;

    .sa-status-label__icon {
      min-width: 18px;
      min-height: 18px;
      margin-right: 5px;
    }
  }
</style>
