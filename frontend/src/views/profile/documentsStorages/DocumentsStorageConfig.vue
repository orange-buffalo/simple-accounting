<template>
  <div class="documents-storage-config">
    <div class="documents-storage-config__header">
      <el-switch v-model="enabled"
                 @change="onEnabledChange">
      </el-switch>
      <h3>{{storageName}}</h3>
    </div>
    <slot v-if="enabled"/>
  </div>
</template>

<script>
  export default {
    name: 'DocumentsStorageConfig',

    props: {
      storageName: String,
      storageId: String,
      userDocumentsStorage: String
    },

    data: function () {
      return {
        enabled: false
      }
    },

    created: function () {
      this._setEnabled()
    },

    methods: {
      _setEnabled: function () {
        this.enabled = this.storageId === this.userDocumentsStorage
      },

      onEnabledChange: function () {
        this.$emit(this.enabled ? 'storage-enabled' : 'storage-disabled', this.storageId)
      }
    },

    watch: {
      userDocumentsStorage: function () {
        this._setEnabled()
      }
    }
  }
</script>

<style lang="scss">

  .documents-storage-config {
    margin-bottom: 20px;

    &__header {
      display: flex;
      align-items: center;
      margin-bottom: 10px;

      h3 {
        display: inline;
        margin: 0 0 0 10px;
      }
    }
  }

</style>


