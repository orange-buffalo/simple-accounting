<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<!--http://www.urbanui.com/chroma/pages/tables/basic-table.html for inspiration-->

<style lang="scss">
  // todo move to proper place
  @import url( "https://fonts.googleapis.com/css?family=Muli" );

  body {
    margin: 0;
    padding: 0;
    font-family: 'Muli',serif;
  }

  #app {
    min-height:100vh;
  }
</style>