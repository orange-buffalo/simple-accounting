// noinspection JSUnusedGlobalSymbols

import { ElOption } from 'element-plus';
import { ref } from 'vue';
import { action } from '@storybook/addon-actions';
import SaFormSelect from '@/components/form/SaFormSelect.vue';
import { throwApiFieldLevelValidationError } from '@/__storybook__/stories-utils';
import SaForm from '@/components/form/SaForm.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf, clickOnElement, waitForText,
} from '@/__storybook__/screenshots';
import SaFormInput from '@/components/form/SaFormInput.vue';

export default {
  title: 'Components/Basic/SaForm',
};

export const Default = defineStory(() => ({
  components: {
    SaForm,
    SaFormInput,
    SaFormSelect,
    ElOption,
  },
  setup() {
    const formValues = ref({
      details: {
        name: 'Fry',
      },
      role: 'USER',
    });
    const onSubmit = async () => {
      action('form-submit')(formValues.value);
      throwApiFieldLevelValidationError({
        field: 'details.name',
        error: 'SizeConstraintViolated',
        message: 'must not be null',
        params: {
          min: '1',
          max: '2557',
        },
      });
    };
    const onCancel = async () => {
      action('form-cancel')();
    };
    return {
      formValues,
      onSubmit,
      onCancel,
    };
  },
  template: `
    <h4>Defaults</h4>
    <SaForm v-model="formValues" :on-submit="onSubmit" :on-cancel="onCancel" id="validationForm">
      <SaFormInput label="Name"
                   prop="details.name"
      />
      <SaFormSelect label="Role"
                    prop="role"
      >
        <ElOption label="User" value="USER" />
        <ElOption label="Admin" value="ADMIN" />
      </SaFormSelect>
    </SaForm>
    {{ JSON.stringify(formValues) }}

    <h4>Custom button labels</h4>
    <SaForm v-model="formValues"
            :on-submit="onSubmit"
            submit-button-label="Submit"
            :on-cancel="onCancel"
            cancel-button-label="Back"
    >
      <SaFormInput label="Name" prop="details.name" />
    </SaForm>

    <h4>No cancel callback</h4>
    <SaForm v-model="formValues"
            :on-submit="onSubmit"
            submit-button-label="Submit"
    >
      <SaFormInput label="Name" prop="details.name" />
    </SaForm>
  `,
}), {
  asPage: true,
  screenshotPreparation: allOf(
    clickOnElement('#validationForm .el-button--primary'),
    waitForText('The length of this value should be no longer than 2,557'),
  ),
});
