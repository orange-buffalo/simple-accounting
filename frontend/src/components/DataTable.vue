<template>
  <div>
    <ElTable
      :data="data"
      v-bind="$props"
      @sort-change="onSortChange"
    >
      <slot />
    </ElTable>

    <ElPagination
      :page-sizes="[10, 20, 30, 100]"
      :page-size="10"
      layout="sizes, prev, pager, next"
      :total="totalElements"
      @size-change="onPageSizeChange"
      @current-change="onCurrentPageChange"
    />
  </div>
</template>

<script>
  import { api } from '@/services/api';

  export default {
    name: 'DataTable',

    props: {
      apiPath: {
        type: String,
        required: true,
      },

      // Element table properties to pass through
      stripe: Boolean,
    },

    data() {
      return {
        totalElements: 0,
        data: [],
      };
    },

    created() {
      this.currentPage = 1;
      this.pageSize = 10;
      this.reloadData();
    },

    methods: {
      onPageSizeChange(val) {
        this.pageSize = val;
        this.reloadData();
      },

      onCurrentPageChange(val) {
        this.currentPage = val;
        this.reloadData();
      },

      onSortChange(event) {
        console.error(event);
      },

      async reloadData() {
        if (this.cancelToken) {
          this.cancelToken.cancel();
        }

        this.cancelToken = api.createCancelToken();

        const page = await api.pageRequest(this.apiPath)
          .limit(this.pageSize)
          .page(this.currentPage)
          .config({
            cancelToken: this.cancelToken.token,
          })
          .getPage();

        this.data = page.data;
        this.totalElements = page.totalElements;
        this.cancelToken = null;
      },
    },
  };
</script>
