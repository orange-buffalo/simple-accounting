<template>
  <div>
    <el-button @click="navigateToNewCategoryView">Add new</el-button>
    <el-card class="box-card" v-for="category in categories">
      <div slot="header" class="clearfix">
        <span>{{category.name}}</span>
        <el-button style="float: right; padding: 3px 0" type="text">Operation button</el-button>
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
    </el-card>
  </div>
</template>

<script>
  import {mapState} from 'vuex'

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
