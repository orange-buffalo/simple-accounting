<template>
  <div>
    <el-table
        :data="data"
        v-bind="$props"
        @sort-change="onSortChange">
      <slot></slot>
    </el-table>

    <el-pagination
        @size-change="onPageSizeChange"
        @current-change="onCurrentPageChange"
        :page-sizes="[10, 20, 30, 100]"
        :page-size="10"
        layout="sizes, prev, pager, next"
        :total="totalElements">
    </el-pagination>
  </div>
</template>

<script>
  import api from '@/services/api'

  export default {
    name: 'DataTable',
    
    props: {
      apiPath: {
        type: String,
        required: true
      },

      // Element table properties to pass through
      stripe: Boolean
    },

    data: function () {
      return {
        totalElements: 0,
        data: []
      }
    },

    created: function () {
      api.get(this.apiPath).then(response => {
        this.data = response.data.data
        this.totalElements = response.data.totalElements
      })
    },

    methods: {
      onPageSizeChange: function (val) {
        console.log(`${val} items per page`);
      },

      onCurrentPageChange: function (val) {
        console.log(`current page: ${val}`);
      },

      onSortChange: function (event) {
        console.error(event)
      }
    }
  }
</script>