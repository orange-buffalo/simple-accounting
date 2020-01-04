<template>
  <div class="sa-document">
    <SaIcon
      :icon="documentTypeIcon"
      class="sa-document__file-icon"
    />

    <div class="sa-document__file-description">
      <div class="sa-document__file-description__header">
        <span
          :title="documentName"
          class="sa-document__file-description__header__file-name"
        >{{ documentName }}</span>

        <SaIcon
          v-if="removable"
          class="sa-document__file-description__header__remove-icon"
          icon="delete"
          @click="onRemove"
        />
      </div>

      <div class="sa-document__file-description__file-extras">
        <slot name="extras">
          <SaDocumentDownloadLink
            v-if="documentId"
            :document-name="documentName"
            :document-id="documentId"
            class="sa-document__file-description__file-extras__download-link"
          />
        </slot>
        <span v-if="documentSizeInBytes">{{ $t('saDocument.size.label', [documentSizeInBytes]) }}</span>
      </div>

      <ElProgress
        v-if="inProgress"
        :percentage="progress"
      />
    </div>
  </div>
</template>

<script>
  import SaIcon from '@/components/SaIcon';
  import SaDocumentDownloadLink from '@/components/documents/SaDocumentDownloadLink';

  export default {
    name: 'SaDocument',
    components: {
      SaDocumentDownloadLink,
      SaIcon,
    },

    props: {
      documentName: {
        type: String,
        default: null,
      },

      documentId: {
        type: Number,
        default: null,
      },

      removable: {
        type: Boolean,
        default: false,
      },

      inProgress: {
        type: Boolean,
        default: false,
      },

      progress: {
        type: Number,
        default: null,
      },

      documentSizeInBytes: {
        type: Number,
        default: null,
      },
    },

    computed: {
      documentTypeIcon() {
        const fileName = this.documentName.toLowerCase();
        if (fileName.endsWith('.pdf')) {
          return 'pdf';
        }
        if (fileName.endsWith('.jpg') || fileName.endsWith('.jpeg')) {
          return 'jpg';
        }
        if (fileName.endsWith('.zip') || fileName.endsWith('.gz') || fileName.endsWith('.rar')) {
          return 'zip';
        }
        if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) {
          return 'doc';
        }
        return 'file';
      },
    },

    methods: {
      onRemove() {
        this.$emit('removed');
      },
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";

  .sa-document {
    display: flex;

    .el-progress__text {
      color: $secondary-text-color;
      font-size: 80% !important;
    }

    &__file-icon {
      margin-right: 5px;
      width: 40px;
      height: 40px;
      flex-shrink: 0;
    }

    &__file-description {
      width: 100%;
      line-height: 1em;
      overflow: hidden;

      &__header {
        display: flex;
        justify-content: flex-start;
        line-height: 1em;
        margin-bottom: 5px;

        &__file-name {
          display: inline-block;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          margin-right: 5px;
        }

        &__remove-icon {
          cursor: pointer;
          color: $components-color;
          width: 10px;
          height: 10px;
          margin-left: auto;
          flex-shrink: 0;
        }
      }

      &__file-extras {
        color: $secondary-text-color;
        font-size: 90%;
        display: flex;
        align-items: center;
        flex-wrap: wrap;

        &__download-link {
          padding: 0 5px 0 0;
        }
      }
    }
  }
</style>
