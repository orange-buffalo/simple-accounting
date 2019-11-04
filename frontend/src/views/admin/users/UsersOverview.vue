<template>
  <div>
    <RouterLink to="/">
      Home
    </RouterLink>
    <br>
    <RouterLink to="/admin/users/create">
      Add User
    </RouterLink>
    <br>
    <DataTable
      stripe
      api-path="/users"
    >
      <ElTableColumn
        prop="userName"
        label="Username"
      />
      <ElTableColumn
        label="Is Administrator?"
      >
        <template slot-scope="scope">
          {{ scope.row.admin ? 'Yes' : 'No' }}
        </template>
      </ElTableColumn>
    </DataTable>
  </div>
</template>

<script>
  import DataTable from '@/components/DataTable';

  export default {
    name: 'UsersOverview',
    components: {
      DataTable,
    },
  };
</script>
