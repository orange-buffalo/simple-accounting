<template>
  <div class="documents-storage-config">
    <div class="documents-storage-config__header">
      <ElSwitch
        v-model="enabled"
        @change="onEnabledChange"
      />
      <h4>{{ storageName }}</h4>
    </div>
    <slot v-if="enabled" />
  </div>
</template>

<script>
  export default {
    name: 'DocumentsStorageConfig',

    props: {
      storageName: String,
      storageId: String,
      userDocumentsStorage: String,
    },

    data() {
      return {
        enabled: false,
      };
    },

    watch: {
      userDocumentsStorage() {
        this._setEnabled();
      },
    },

    created() {
      this._setEnabled();
    },

    methods: {
      _setEnabled() {
        this.enabled = this.storageId === this.userDocumentsStorage;
      },

      onEnabledChange() {
        this.$emit(this.enabled ? 'storage-enabled' : 'storage-disabled', this.storageId);
      },
    },
  };
</script>

<style lang="scss">

  .documents-storage-config {
    margin-bottom: 20px;

    &__header {
      display: flex;
      align-items: center;
      margin-bottom: 10px;

      h4 {
        display: inline;
        margin: 0 0 0 10px;
      }
    }
  }

</style>
