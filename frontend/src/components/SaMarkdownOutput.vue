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
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div v-html="renderedMarkdown" />
  </div>
</template>

<script lang="ts" setup>
import DOMPurify from 'dompurify';
import { debounce } from 'lodash';
import { marked } from 'marked';
import { computed, ref, watch } from 'vue';

function renderMarkdown(source?: string) {
  return source ? DOMPurify.sanitize(marked.parse(source) as string) : '';
}

const props = withDefaults(
  defineProps<{
    source?: string;
    preview?: boolean;
  }>(),
  {
    preview: false,
  },
);

const renderedMarkdown = ref<string | undefined>();
const updateMarkdown = debounce(() => {
  renderedMarkdown.value = renderMarkdown(props.source);
}, 300);

const panelClass = computed(() => ({
  'markdown-output_preview': props.preview,
}));

watch(() => props.source, updateMarkdown, { immediate: true });
</script>

<style lang="scss">
  /*todo #73: common component refers to app styles - redesign dependencies  */
  @use "@/styles/vars.scss" as *;

  .markdown-output {
    p {
      margin-top: 0;
      margin-bottom: 5px;
    }

    a {
      color: $components-color;
      text-decoration: none;
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
