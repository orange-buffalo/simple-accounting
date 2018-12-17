<template>
  <div class="data-items">
    <el-pagination
        v-if="paginator && totalElements > 0"
        @current-change="onCurrentPageChange"
        :current-page.sync="currentPage"
        :page-size="pageSize"
        layout="prev, pager, next"
        :total="totalElements">
    </el-pagination>

    <el-row :gutter="10" v-bind="$props"
            :class="loading ? 'loading' : ''">
      <el-col v-bind="$props"
              v-for="dataItem in data"
              :key="dataItem.id">

        <slot v-bind:item="dataItem"/>
      </el-col>
    </el-row>

    <div v-if="totalElements === 0 && !loading"
         class="empty-results">
      <svgicon name="empty-box"/>
      <span>No results here</span>
    </div>

    <div v-if="totalElements === 0 && loading"
         class="loading-bar">
      <i class="el-icon-loading"></i>
      <span>Loading..</span>
    </div>

    <el-pagination
        v-if="paginator && totalElements > 0"
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
  import '@/components/icons/empty-box'

  export default {
    name: 'DataItems',

    components: {},

    props: {
      apiPath: {
        type: String,
        required: true
      },

      filters: Object,

      paginator: {
        type: Boolean,
        default: true
      },

      // Element row and column properties to pass through
      lg: Number
    },

    data: function () {
      return {
        totalElements: 0,
        data: [],
        pageSize: 10,
        currentPage: 1,
        loading: false
      }
    },

    created: function () {
      this.reloadData()
    },

    methods: {
      onCurrentPageChange: function () {
        this.reloadData()
      },

      onSortChange: function (event) {
        console.error(event)
      },

      reloadData: async function () {
        this.loading = true

        if (this.cancelToken) {
          this.cancelToken.cancel()
        }

        this.cancelToken = api.createCancelToken()

        let pageRequest = api.pageRequest(this.apiPath)
            .limit(this.paginator ? this.pageSize : 500)
            .page(this.paginator ? this.currentPage : 1)
            .config({
              cancelToken: this.cancelToken.token
            })

        if (this.filters) {
          this.filters.applyToRequest(pageRequest)
        }

        try {
          let page = await pageRequest.getPage()

          this.data = page.data
          this.totalElements = page.totalElements
          this.cancelToken = null
          this.loading = false
        } catch (e) {
          if (!api.isCancel(e)) {
            this.loading = false
            //todo toaster instead
            throw e;
          }
        }
      }
    }
  }
</script>

<style lang="scss">
  .data-items {
    .el-row {
      transition: opacity 0.3s;

      &.loading {
        opacity: 0.5;
      }
    }

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

    .empty-results {
      display: flex;
      flex-flow: column;
      align-items: center;
      color: #9e9e9e;

      .svg-icon {
        width: 48px;
        height: 48px;
        margin: 10px;
      }
    }

    .loading-bar {
      display: flex;
      flex-flow: column;
      align-items: center;
      color: #9e9e9e;

      i {
        font-size: 300%;
        margin: 10px;
      }
    }
  }
</style>