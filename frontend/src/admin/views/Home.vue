<template>
    <div>
      <router-link to="/about">About</router-link>
    <hr/>
    <HelloWorld msg="Welcome to ADMIN App"/>

    </div>
</template>

<script>
// @ is an alias to /src
import HelloWorld from '@/components/HelloWorld'

export default {
  name: 'home',
  components: {
    HelloWorld
  }
}
</script>
