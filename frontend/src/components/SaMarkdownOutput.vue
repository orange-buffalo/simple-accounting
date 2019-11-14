<template>
  <div
    class="markdown-output"
    :class="panelClass"
  >
    <div
      v-if="preview"
      class="markdown-output__preview-label"
    >
      Preview
    </div>
    <div v-html="renderedMarkdown" />
  </div>
</template>

<script>
  import DOMPurify from 'dompurify';
  import marked from 'marked';
  import debounce from 'lodash/debounce';

  function renderMarkdown(source) {
    return source ? DOMPurify.sanitize(marked(source)) : '';
  }

  export default {
    name: 'SaMarkdownOutput',

    props: {
      source: {
        type: String,
      },
      preview: {
        type: Boolean,
        default: false,
      },
    },

    data() {
      return {
        renderedMarkdown: renderMarkdown(this.source),
        updateMarkdown: debounce(() => {
          this.renderedMarkdown = renderMarkdown(this.source);
        }, 300),
      };
    },

    computed: {
      panelClass() {
        return {
          'markdown-output_preview': this.preview,
        };
      },
    },

    watch: {
      source() {
        this.updateMarkdown();
      },
    },
  };
</script>

<style lang="scss">
  /*todo #73: common component refers to app styles - redesign dependencies  */
  @import "@/styles/vars.scss";

  .markdown-output {
    p {
      margin-top: 0;
      margin-bottom: 5px;
    }

    a {
      color: $components-color;
      text-decoration: none ;
    }

    &_preview {
      padding: 10px;
      border: 1px solid $components-border-color;
      border-radius: 3px;
    }

    &__preview-label {
      font-style: italic;
      margin-bottom: 3px;
      color: $secondary-text-color;
      font-size: 70%;
      text-align: right;
    }
  }
</style>
