// noinspection JSUnusedGlobalSymbols

import { ElButton, ElFormItem, ElInput } from 'element-plus';
import { defineComponent, ref } from 'vue';
import { action } from '@storybook/addon-actions';
import { delay } from '@/__storybook__/stories-utils';
import { useForm, useFormWithDocumentsUpload } from '@/components/form/use-form';
import SaForm from '@/components/form/SaForm.vue';
import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf, clickOnElement, waitForText,
} from '@/__storybook__/screenshots';
import { mockSuccessStorageStatus } from '@/__storybook__/api-mocks';

export default {
  title: 'Components/SaForm',
};

const StoryForm = defineComponent({
  props: {
    neverEndingLoading: {
      type: Boolean,
      default: false,
    },
  },
  components: {
    SaForm,
    ElFormItem,
    ElInput,
    ElButton,
  },
  setup: (props) => {
    const fry = ref({
      name: 'Fry',
    });

    const loadData = async () => {
      action('form-load')();
      if (props.neverEndingLoading) await delay(999999999);
      fry.value.name = 'Fry';
    };

    const saveData = async () => {
      action('form-submit')();
      fry.value = {
        name: 'Zoidberg',
      };
    };

    return ({
      fry,
      rules: {
        name: {
          required: true,
          message: 'Please provide name',
        },
      },
      ...useForm(loadData, saveData),
    });
  },
  template: `
    <div>
    <SaForm :model="fry"
            :rules="rules"
            ref="formRef"
    >
      <ElFormItem
        label="Name"
        prop="name"
      >
        <ElInput v-model="fry.name" />
      </ElFormItem>

      <template #buttons-bar>
        <ElButton>Cancel</ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          Save
        </ElButton>
      </template>
    </SaForm>
    <div v-if="!neverEndingLoading" style="padding: 20px">
      {{ fry.name }}
    </div>
    </div>
  `,
});

const StoryFormWithDocuments = defineComponent({
  components: {
    SaForm,
    ElFormItem,
    ElInput,
    ElButton,
    SaDocumentsUpload,
  },
  setup: () => {
    const fry = ref({});

    const loadData = async () => {
      // noop
    };
    const saveData = async () => {
      action('form-submit')();
      fry.value = {
        name: 'Fry',
      };
    };

    const documents : number[] = [];

    return ({
      fry,
      ...useFormWithDocumentsUpload(loadData, saveData),
      documents,
    });
  },
  template: `
    <div>
    <SaForm :model="fry"
            ref="formRef"
    >
      <ElFormItem
        label="Name"
        prop="name"
      >
        <ElInput v-model="fry.name" />
      </ElFormItem>

      <SaDocumentsUpload :documents-ids="documents"
                         @uploads-failed="onDocumentsUploadFailure"
                         @uploads-completed="onDocumentsUploadComplete"
                         ref="documentsUploadRef"
      />

      <template #buttons-bar>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          Save
        </ElButton>
      </template>
    </SaForm>
    <div style="padding: 20px">
      {{ fry.name }}
    </div>
    </div>
  `,
});

export const Default = defineStory(() => ({
  components: {
    StoryForm,
    StoryFormWithDocuments,
  },
  beforeCreate() {
    mockSuccessStorageStatus();
  },
  template: `
    <h4>Loading</h4>
    <StoryForm never-ending-loading id="loadingForm" />

    <h4>Active</h4>
    <StoryForm id="activeForm" />

    <h4>With Document Upload</h4>
    <StoryFormWithDocuments id="documentsForm" />
  `,
}), {
  screenshotPreparation: allOf(
    clickOnElement('#activeForm .el-button--primary'),
    clickOnElement('#documentsForm .el-button--primary'),
    waitForText('Zoidberg', '#activeForm'),
    waitForText('Fry', '#documentsForm'),
  ),
  asPage: true,
});
