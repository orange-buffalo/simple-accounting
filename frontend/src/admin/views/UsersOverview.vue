<template>
  <div>
    <router-link to="/">Home</router-link>
    <br/>
    <data-table stripe>
      <el-table-column
          prop="date"
          label="Date"
          sortable="custom"
          width="180">
      </el-table-column>
      <el-table-column
          prop="name"
          label="Name"
          sortable="custom"
          width="180">
      </el-table-column>
      <el-table-column
          prop="address"
          sortable="custom"
          label="Address">
      </el-table-column>
    </data-table>
  </div>
</template>

<script>
  import DataTable from '@/components/DataTable'

  export default {
    name: 'UsersOverview',
    components: {
      DataTable
    }
  }
</script>
