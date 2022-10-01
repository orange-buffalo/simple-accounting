<template>
  <div>
    <h2>{{ $t.myProfile.languagePreferences.header() }}</h2>

    <div class="row">
      <div class="col col-xs-12 col-lg-6">
        <ElFormItem
          :label="$t.myProfile.languagePreferences.language.label()"
          prop="language"
        >
          <ElSelect
            v-model="inputLanguage"
            :placeholder="$t.myProfile.languagePreferences.language.placeholder()"
            @change="updateLanguage"
          >
            <ElOption
              v-for="availableLanguage in languages"
              :key="availableLanguage.languageCode"
              :label="availableLanguage.displayName"
              :value="availableLanguage.languageCode"
            />
          </ElSelect>
        </ElFormItem>
      </div>

      <div class="col col-xs-12 col-lg-6">
        <ElFormItem
          :label="$t.myProfile.languagePreferences.locale.label()"
          prop="locale"
        >
          <ElSelect
            v-model="inputLocale"
            :filterable="true"
            :placeholder="$t.myProfile.languagePreferences.locale.placeholder()"
            @change="updateLocale"
          >
            <ElOption
              v-for="availableLocale in locales"
              :key="availableLocale.locale"
              :label="availableLocale.displayName"
              :value="availableLocale.locale"
            />
          </ElSelect>
        </ElFormItem>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import type { SupportedLanguage, SupportedLocale } from '@/services/i18n';
  import {
    $t, getSupportedLanguages, getSupportedLocales, localeIdToLanguageTag, languageTagToLocaleId, setLocaleFromProfile,
  } from '@/services/i18n';

  const props = defineProps<{
    language: string,
    locale: string,
  }>();

  const emit = defineEmits<{(e: 'update:language', language: string): void,
                            (e: 'update:locale', locale: string): void;
  }>();

  const languages = ref<Array<SupportedLanguage>>(getSupportedLanguages());
  const locales = ref<Array<SupportedLocale>>(getSupportedLocales());
  const inputLanguage = ref<string>(localeIdToLanguageTag(props.language));
  const inputLocale = ref<string>(localeIdToLanguageTag(props.locale));

  watch(() => props.language, () => {
    inputLanguage.value = localeIdToLanguageTag(props.language);
  });
  watch(() => props.locale, () => {
    inputLocale.value = localeIdToLanguageTag(props.locale);
  });

  const updateLanguage = async () => {
    // todo #204: loading indicator while locale/language is loaded and applied
    await emit('update:language', languageTagToLocaleId(inputLanguage.value));
    await setLocaleFromProfile(props.locale, languageTagToLocaleId(inputLanguage.value));
    locales.value = getSupportedLocales();
  };

  const updateLocale = async () => {
    await emit('update:locale', languageTagToLocaleId(inputLocale.value));
    await setLocaleFromProfile(languageTagToLocaleId(inputLocale.value), props.language);
  };
</script>
