<template>
  <div>
    <h2>{{ $t.myProfile.languagePreferences.header() }}</h2>

    <SaForm
      v-model="formValues"
      :on-submit="submitLanguagePreferences"
      :external-loading="props.loading"
      :hide-buttons="true"
    >
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
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { ElOption } from 'element-plus';
  import type { SupportedLanguage, SupportedLocale } from '@/services/i18n';
  import {
    $t, getSupportedLanguages, getSupportedLocales, localeIdToLanguageTag, languageTagToLocaleId, setLocaleFromProfile,
  } from '@/services/i18n';
  import { ProfileDto, profileApi } from '@/services/api';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormSelect from '@/components/form/SaFormSelect.vue';
  import useNotifications from '@/components/notifications/use-notifications';

  const props = defineProps<{
    profile: ProfileDto,
    loading: boolean,
  }>();

  const emit = defineEmits<{
    (e: 'profile-updated', profile: ProfileDto): void,
  }>();

  const { showSuccessNotification } = useNotifications();

  const languages = ref<Array<SupportedLanguage>>(getSupportedLanguages());
  const locales = ref<Array<SupportedLocale>>(getSupportedLocales());

  type LanguagePreferencesFormValues = {
    language: string,
    locale: string,
  };

  const formValues = ref<LanguagePreferencesFormValues>({
    language: localeIdToLanguageTag(props.profile.i18n.language),
    locale: localeIdToLanguageTag(props.profile.i18n.locale),
  });

  watch(() => props.profile, () => {
    formValues.value.language = localeIdToLanguageTag(props.profile.i18n.language);
    formValues.value.locale = localeIdToLanguageTag(props.profile.i18n.locale);
  }, { deep: true });

  const submitLanguagePreferences = async () => {
    const updatedProfile: ProfileDto = {
      ...props.profile,
      i18n: {
        language: languageTagToLocaleId(formValues.value.language),
        locale: languageTagToLocaleId(formValues.value.locale),
      },
    };
    await profileApi.updateProfile({
      updateProfileRequestDto: updatedProfile,
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
