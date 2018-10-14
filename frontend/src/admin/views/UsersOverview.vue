<template>
  <div>
    <router-link to="/">Home</router-link>
    <br/>
    <router-link to="/users/create">Add User</router-link>
    <br/>
    <data-table stripe api-path="/admin/users">
      <el-table-column
          prop="userName"
          label="Username"
         >
      </el-table-column>
      <el-table-column
          label="Is Administrator?"
         >
        <template slot-scope="scope">
          {{ scope.row.admin ? 'Yes' : 'No' }}
      </template>
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
