<template>
  <div class="sa-form">
    <ElForm
      ref="elForm"
      label-position="top"
      v-bind="formProps"
      v-on="$listeners"
    >
      <slot />

      <hr>

      <div class="sa-form__buttons-bar">
        <slot name="buttons-bar" />
      </div>
    </ElForm>
  </div>
</template>

<script>
  export default {
    name: 'SaForm',

    computed: {
      formProps() {
        const { labelPosition, ...otherProps } = this.$attrs;
        return otherProps;
      },
    },

    methods: {
      validate(formItem) {
        return this.$refs.elForm.validate(formItem);
      },
    },
  };
</script>

<style lang="scss">
  /*todo #73: common component refers to app styles - redesign dependencies  */
  @import "@/styles/vars.scss";
  @import "@/styles/mixins.scss";

  .sa-form {
    padding: 20px;
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 2px;
    overflow: hidden;

    .el-form {
      width: 100%;
    }

    .el-form-item {
      margin-bottom: 15px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .sa-documents_upload {
      margin-top: 15px;
    }

    .el-select {
      @include input-width;
    }

    .el-input {
      @include input-width;
    }

    .el-form-item__label {
      padding: 0;
    }

    .el-input {
      .el-input-number & {
        width: 100%;
      }
    }

    h2 {
      margin-bottom: 10px;
    }

    hr {
      border: 1px solid $primary-grey;
      margin-top: 10px;
      margin-bottom: 10px;
    }

    &__buttons-bar {
      margin-top: 20px;
      display: flex;
      justify-content: space-between;
    }
  }
</style>
