<template>
  <div>
    <ElInput
      v-model="notes"
      :placeholder="placeholder"
      type="textarea"
      rows="5"
      class="sa-notes-input__input"
    />

    <SaMarkdownOutput
      v-if="notes"
      :source="notes"
      preview
      class="sa-notes-input__preview"
    />
  </div>
</template>

<script>
  import SaMarkdownOutput from '@/components/SaMarkdownOutput';

  export default {
    name: 'SaNotesInput',

    components: {
      SaMarkdownOutput,
    },

    props: {
      value: {
        type: String,
        default: null,
      },

      placeholder: {
        type: String,
        default: null,
      },
    },

    data() {
      return {
        notes: this.value,
      };
    },

    watch: {
      notes(val) {
        this.$emit('input', val);
      },

      value(val) {
        this.notes = val;
      },
    },
  };
</script>

<style lang="scss">
  .sa-notes-input {
    &__input {
      width: 100% !important;
    }

    &__preview {
      margin-top: 10px;
      line-height: 1em;
      padding: 5px 15px;
    }
  }
</style>
