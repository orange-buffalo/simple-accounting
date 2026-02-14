<template>
  <div class="sa-document">
    <template v-if="loading">
      <div class="sa-document__loader__file-icon" />
      <div class="sa-document__loader__file-description">
        <div class="sa-document__loader__file-description__header" />
        <div class="sa-document__loader__file-description__link" />
        <div class="sa-document__loader__file-description__size" />
      </div>
    </template>

    <template v-else>
      <SaIcon
        :size="40"
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
          <span v-if="documentSizeInBytes">{{ $t.saDocument.size.label(documentSizeInBytes) }}</span>
        </div>

        <ElProgress
          v-if="inProgress"
          :percentage="progress"
          :format="progressFormat"
        />
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import SaDocumentDownloadLink from '@/components/documents/SaDocumentDownloadLink.vue';
import SaIcon from '@/components/SaIcon.vue';
import { $t } from '@/services/i18n';

function getDocumentTypeIcon(documentName: string) {
  const fileName = documentName.toLowerCase();
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
}

const props = withDefaults(
  defineProps<{
    documentName?: string;
    documentId?: number;
    removable?: boolean;
    inProgress?: boolean;
    progress?: number;
    documentSizeInBytes?: number;
    loading?: boolean;
  }>(),
  {
    documentName: '',
    removable: false,
    inProgress: false,
    loading: false,
  },
);

const emit = defineEmits<{ (e: 'removed'): void }>();

const documentTypeIcon = computed(() => getDocumentTypeIcon(props.documentName));

const onRemove = () => emit('removed');

const progressFormat = (percent: number) => $t.value.common.percent(percent / 100);
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-document {
    display: flex;

    .el-progress--line {
      display: flex;
      align-items: center;
    }

    .el-progress__text {
      color: $secondary-text-color;
      font-size: 80% !important;
    }

    .el-progress-bar {
      margin-right: 0;
      padding-right: 0;
    }

    &__file-icon {
      margin-right: 5px;
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
          .el-button {
            padding: 0 5px 0 0 !important;
            height: auto;
          }
        }
      }
    }

    &__loader {
      &__file-icon {
        margin-left: 10px;
        margin-right: 5px;
        width: 40px;
        height: 50px;
        flex-shrink: 0;
        @include loading-placeholder;
      }

      &__file-description {
        display: flex;
        width: 100%;
        flex-wrap: wrap;

        div {
          content: '&nbsp;';
          @include loading-placeholder;
          height: 1em;
        }

        &__header {
          width: 80%;
        }

        &__link {
          width: 30%;
          margin-right: 5px;
        }

        &__size {
          width: 10%;
        }
      }
    }
  }
</style>
