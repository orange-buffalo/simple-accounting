<template>
  <SaForm
    v-model="formValues"
    :on-submit="submitLanguagePreferences"
    :external-loading="props.loading"
    :hide-buttons="true"
  >
    <h2>{{ $t.myProfile.languagePreferences.header() }}</h2>

    <div class="row">
      <div class="col col-xs-12 col-lg-6">
        <SaFormSelect
          prop="language"
          :label="$t.myProfile.languagePreferences.language.label()"
          :placeholder="$t.myProfile.languagePreferences.language.placeholder()"
          :submit-on-change="true"
        >
          <ElOption
            v-for="availableLanguage in languages"
            :key="availableLanguage.languageCode"
            :label="availableLanguage.displayName"
            :value="availableLanguage.languageCode"
          />
        </SaFormSelect>
      </div>

      <div class="col col-xs-12 col-lg-6">
        <SaFormSelect
          prop="locale"
          :label="$t.myProfile.languagePreferences.locale.label()"
          :placeholder="$t.myProfile.languagePreferences.locale.placeholder()"
          :filterable="true"
          :submit-on-change="true"
        >
          <ElOption
            v-for="availableLocale in locales"
            :key="availableLocale.locale"
            :label="availableLocale.displayName"
            :value="availableLocale.locale"
          />
        </SaFormSelect>
      </div>
    </div>
  </SaForm>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { ElOption } from 'element-plus';
  import type { SupportedLanguage, SupportedLocale } from '@/services/i18n';
  import {
    $t, getSupportedLanguages, getSupportedLocales, localeIdToLanguageTag, languageTagToLocaleId, setLocaleFromProfile,
  } from '@/services/i18n';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormSelect from '@/components/form/SaFormSelect.vue';
  import useNotifications from '@/components/notifications/use-notifications';
  import { UserProfileQuery } from '@/services/api/gql/graphql.ts';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    profile?: UserProfileQuery["userProfile"],
    loading: boolean,
  }>();

  const emit = defineEmits<{
    (e: 'profile-updated', profile: UserProfileQuery['userProfile']): void,
  }>();

  const { showSuccessNotification } = useNotifications();

  const languages = ref<Array<SupportedLanguage>>(getSupportedLanguages());
  const locales = ref<Array<SupportedLocale>>(getSupportedLocales());

  type LanguagePreferencesFormValues = {
    language: string,
    locale: string,
  };

  const formValues = ref<LanguagePreferencesFormValues>({
    language: '',
    locale: '',
  });

  watch(() => props.profile, () => {
    formValues.value.language = localeIdToLanguageTag(props.profile?.i18n?.language || '');
    formValues.value.locale = localeIdToLanguageTag(props.profile?.i18n?.locale || '');
  }, {
    deep: true,
    immediate: true,
  });

  const updateProfileMutation = useMutation(graphql(/* GraphQL */ `
    mutation updateProfileLanguage($documentsStorage: String, $locale: String!, $language: String!) {
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

  const submitLanguagePreferences = async () => {
    const updatedProfile = await updateProfileMutation({
      documentsStorage: props.profile!.documentsStorage ?? null,
      locale: languageTagToLocaleId(formValues.value.locale),
      language: languageTagToLocaleId(formValues.value.language),
    });
    await setLocaleFromProfile(
      languageTagToLocaleId(formValues.value.locale),
      languageTagToLocaleId(formValues.value.language),
    );
    locales.value = getSupportedLocales();
    emit('profile-updated', updatedProfile);
    showSuccessNotification($t.value.myProfile.languagePreferences.feedback.success());
  };
</script>
