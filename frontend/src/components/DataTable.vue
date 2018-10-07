<template>
  <div>
    <el-table
        :data="tableData"
        v-bind="$props"
        @sort-change="onSortChange"
        >
      <slot></slot>
      
    </el-table>

    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :page-sizes="[10, 20, 30, 100]"
      :page-size="10"
      layout="sizes, prev, pager, next"
      :total="1000">
    </el-pagination>
  </div>
</template>

<script>
  export default {
    name: 'DataTable',
    props: {
      // Element table properties to pass through
      stripe: Boolean
    },
    data: function () {
      return {
        tableData: [{
          date: '2016-05-03',
          name: 'Tom',
          address: 'No. 189, Grove St, Los Angeles'
        }, {
          date: '2016-05-02',
          name: 'Tom',
          address: 'No. 189, Grove St, Los Angeles'
        }, {
          date: '2016-05-04',
          name: 'Tom',
          address: 'No. 189, Grove St, Los Angeles'
        }, {
          date: '2016-05-01',
          name: 'Tom',
          address: 'No. 189, Grove St, Los Angeles'
        }]
      }
    } ,

    methods: {
      handleSizeChange: function(val) {
        console.log(`${val} items per page`);
      },

      handleCurrentChange: function(val) {
        console.log(`current page: ${val}`);
      },

      onSortChange: function (event) {
        console.error(event)
      }
    }
  }
</script>