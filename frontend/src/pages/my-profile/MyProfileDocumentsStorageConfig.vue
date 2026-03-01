<template>
  <SaForm
    v-model="formValues"
    :on-submit="submitStorageConfig"
    :external-loading="props.loading"
    :hide-buttons="true"
  >
    <h2>{{ $t.myProfile.documentsStorage.header() }}</h2>
    <div class="documents-storage-config" :id="`storage-config_${storageId}`">
      <SaFormSwitchSection
        :label="storageName"
        prop="enabled"
        :submit-on-change="true"
      />
      <slot v-if="formValues.enabled" />
    </div>
  </SaForm>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormSwitchSection from '@/components/form/SaFormSwitchSection.vue';
  import { $t } from '@/services/i18n';
  import { UserProfileQuery } from '@/services/api/gql/graphql.ts';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    storageName: string,
    storageId: string,
    profile?: UserProfileQuery['userProfile'],
    loading: boolean,
  }>();

  const emit = defineEmits<{
    (e: 'profile-updated', profile: UserProfileQuery['userProfile']): void,
  }>();

  type StorageConfigFormValues = {
    enabled: boolean,
  };

  const formValues = ref<StorageConfigFormValues>({
    enabled: false,
  });

  watch(() => props.profile, () => {
    formValues.value = {
      enabled: props.storageId === props.profile?.documentsStorage,
    };
  }, {
    deep: true,
    immediate: true,
  });

  const updateProfileMutation = useMutation(graphql(/* GraphQL */ `
    mutation updateProfile($documentsStorage: String, $locale: String!, $language: String!) {
      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {
        documentsStorage
        i18n {
          language
          locale
        }
        userName
      }
    }
  `), 'updateProfile');

  const submitStorageConfig = async () => {
    const updatedProfile = await updateProfileMutation({
      documentsStorage: formValues.value.enabled ? props.storageId : null,
      locale: props.profile!.i18n.locale,
      language: props.profile!.i18n.language,
    });
    emit('profile-updated', updatedProfile);
  };
</script>

<style lang="scss">
  .documents-storage-config {
    margin-bottom: 20px;
  }
</style>
