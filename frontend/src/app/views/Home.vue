<template>
  <app-layout>
    <h1>"Welcome to USER App"</h1>
    <router-link to="/settings/categories">Settings/categories</router-link><br/>
    <router-link to="/expenses">Expenses</router-link><br/>
  </app-layout>
</template>

<script>

  export default {
    name: 'Home',
    components: {}
  }
</script>
