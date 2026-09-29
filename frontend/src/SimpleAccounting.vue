<template>
  <ElConfigProvider :locale="elementPlusLocale">
    <RouterView />
  </ElConfigProvider>
</template>

<script setup lang="ts">
  import { ElConfigProvider } from 'element-plus';
  import { elementPlusLocale } from '@/components/date-picker/date-picker-localization';
</script>
