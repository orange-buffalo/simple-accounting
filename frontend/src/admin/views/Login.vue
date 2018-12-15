<template>
  <login-form @login="onLogin"/>
</template>

<script>

  import LoginForm from '@/components/LoginForm'

  export default {
    name: 'Login',

    components: {
      LoginForm
    },

    methods: {
      onLogin: function () {
        this.$router.push('/')
      }
    }
  }
</script>