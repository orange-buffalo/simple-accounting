<template>
  <div class="categories-overview">
    <div class="sa-page-header">
      <h1>Categories</h1>
    </div>

    <div class="top-buttons-bar">
      <el-button round
                 @click="navigateToNewCategoryView">
        <svgicon name="plus-thin"/>
        Add new
      </el-button>
    </div>

    <div class="sa-item-info-panel" v-for="category in categories">
      <div class="sa-item-title-panel">
        <h3>{{category.name}}</h3>

        <span class="sa-item-edit-link">
          <!--<pencil-icon/>-->
          <el-button type="text"
          >Edit</el-button>
        </span>
      </div>
      <p>
        {{category.description}}
      </p>
      <p>
        Income: {{category.income}}
      </p>
      <p>
        Expense: {{category.expense}}
      </p>
    </div>
  </div>
</template>

<script>
  import {mapState} from 'vuex'
  import '@/components/icons/plus-thin'

  export default {
    name: 'Categories',

    computed: {
      ...mapState({
        categories: state => state.workspaces.currentWorkspace.categories
      })
    },

    methods: {
      navigateToNewCategoryView: function () {
        this.$router.push({name: 'create-new-category'})
      }
    }
  }
</script>

<style lang="scss">
  .categories-overview {
    .sa-item-info-panel {
      margin-bottom: 20px;
    }
  }

  .top-buttons-bar {
    margin-bottom: 30px;
    margin-top: -10px;
  }
</style>
