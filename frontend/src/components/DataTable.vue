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
      this.currentPage = 1
      this.pageSize = 10
      this.reloadData()
    },

    methods: {
      onPageSizeChange: function (val) {
        this.pageSize = val
        this.reloadData()
      },

      onCurrentPageChange: function (val) {
        this.currentPage = val
        this.reloadData()
      },

      onSortChange: function (event) {
        console.error(event)
      },

      reloadData: function () {
        if (this.cancelToken) {
          this.cancelToken.cancel()
        }

        this.cancelToken = api.createCancelToken()

        api.get(this.apiPath, {
          params: {
            limit: this.pageSize,
            page: this.currentPage
          },
          cancelToken: this.cancelToken.token

        }).then(response => {
          this.data = response.data.data
          this.totalElements = response.data.totalElements
          this.cancelToken = null
        })
      }
    }
  }
</script>