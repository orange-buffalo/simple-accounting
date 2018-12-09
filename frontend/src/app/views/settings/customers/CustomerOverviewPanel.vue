<template>
  <div class="customer-panel">
    <div class="customer-info">
      <div class="item-title-panel">
        <h3>{{customer.name}}</h3>
        <span class="item-edit-link">
          <pencil-icon/>
          <el-button type="text"
                     @click="navigateToCustomerEdit">Edit</el-button>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
  import {mapState} from 'vuex'
  import MoneyOutput from '@/app/components/MoneyOutput'
  import DocumentLink from '@/app/components/DocumentLink'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'
  import CalendarImportIcon from 'vue-material-design-icons/CalendarImport'
  import FormatListBulletedTypeIcon from 'vue-material-design-icons/FormatListBulletedType'
  import CashIcon from 'vue-material-design-icons/Cash'
  import PercentIcon from 'vue-material-design-icons/Percent'
  import MessageBulletedIcon from 'vue-material-design-icons/MessageBulleted'
  import PaperclipIcon from 'vue-material-design-icons/Paperclip'
  import PencilIcon from 'vue-material-design-icons/Pencil'

  export default {
    name: 'CustomerOverviewPanel',

    mixins: [withMediumDateFormatter],

    components: {
      MoneyOutput,
      DocumentLink,
      CalendarImportIcon,
      FormatListBulletedTypeIcon,
      CashIcon,
      PercentIcon,
      MessageBulletedIcon,
      PaperclipIcon,
      PencilIcon
    },

    props: {
      customer: Object
    },

    data: function () {
      return {}
    },

    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id
      })
    },

    methods: {
      navigateToCustomerEdit: function () {
        this.$router.push({name: 'edit-customer', params: {id: this.customer.id}})
      }
    }
  }
</script>

<style lang="scss">
  @import "@/app/main.scss";

  .customer-panel {
    display: flex;
    justify-content: space-between;
  }

  .customer-info {
    @extend .item-info-panel;
    flex-grow: 1;
  }
</style>
