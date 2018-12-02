<template>
  <app-layout>
    <router-view></router-view>
  </app-layout>
</template>

<script>
  import AppLayout from '@/app/components/AppLayout'

  export default {
    name: 'UserApp',

    components: {
      AppLayout
    }
  }
</script>
