<template>
  <div class="the-workspace-selector">
    {{currentWorkspace.name}}
  </div>
</template>

<script>
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'
  import '@/components/icons/gear'
  import '@/components/icons/plus-thin'
  import {assign} from 'lodash'
  import api from '@/services/api'
  import CurrencyInput from './CurrencyInput'

  export default {
    name: 'TheWorkspaceSelector',

    mixins: [withWorkspaces]
  }
</script>


<style lang="scss">
  @import "@/app/styles/vars.scss";

  .the-workspace-selector {
    text-align: center;
    margin-bottom: 20px;
    color: $accent-contrast-color;
  }
</style>