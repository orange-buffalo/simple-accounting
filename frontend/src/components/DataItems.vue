<template>
  <div class="data-items">
    <el-pagination
        @current-change="onCurrentPageChange"
        :current-page.sync="currentPage"
        :page-size="pageSize"
        layout="prev, pager, next"
        :total="totalElements">
    </el-pagination>

    <el-row :gutter="10" v-bind="$props">
      <el-col v-bind="$props"
              v-for="dataItem in data"
              :key="dataItem.id">

        <slot v-bind:item="dataItem"/>
      </el-col>
    </el-row>

    <el-pagination
        @current-change="onCurrentPageChange"
        :current-page.sync="currentPage"
        :page-size="pageSize"
        layout="prev, pager, next"
        :total="totalElements">
    </el-pagination>
  </div>
</template>

<script>
  import api from '@/services/api'

  export default {
    name: 'DataItems',

    props: {
      apiPath: {
        type: String,
        required: true
      },

      // Element row and column properties to pass through
      lg: Number
    },

    data: function () {
      return {
        totalElements: 0,
        data: [],
        pageSize: 10,
        currentPage: 1
      }
    },

    created: function () {
      this.reloadData()
    },

    methods: {
      onPageSizeChange: function (val) {
        this.pageSize = val
        this.reloadData()
      },

      onCurrentPageChange: function (val) {
        this.reloadData()
      },

      onSortChange: function (event) {
        console.error(event)
      },

      reloadData: async function () {
        if (this.cancelToken) {
          this.cancelToken.cancel()
        }

        this.cancelToken = api.createCancelToken()

        let page = await api.pageRequest(this.apiPath)
            .limit(this.pageSize)
            .page(this.currentPage)
            .config({
              cancelToken: this.cancelToken.token
            })
            .getPage()

        this.data = page.data
        this.totalElements = page.totalElements
        this.cancelToken = null
      }
    }
  }
</script>

<style lang="scss">
  .data-items {
    .el-col {
      margin-bottom: 20px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .el-pagination {
      text-align: right;

      .btn-prev, .btn-next, .el-pager li {
        background-color: transparent;
      }
    }
  }
</style>