<template>
  <div>
    <h2>{{ $t('myProfile.languagePreferences.header') }}</h2>

    <div class="row">
      <div class="col col-xs-12 col-lg-6">
        <ElFormItem
          :label="$t('myProfile.languagePreferences.language.label')"
          prop="language"
        >
          <ElSelect
            v-model="inputLanguage"
            :placeholder="$t('myProfile.languagePreferences.language.placeholder')"
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
          :label="$t('myProfile.languagePreferences.locale.label')"
          prop="locale"
        >
          <ElSelect
            v-model="inputLocale"
            :filterable="true"
            :placeholder="$t('myProfile.languagePreferences.locale.placeholder')"
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

<script>
  import i18n from '@/services/i18n';

  export default {
    name: 'MyProfileLanguagePreferences',

    props: {
      language: {
        default: null,
        type: String,
      },
      locale: {
        default: null,
        type: String,
      },
    },

    data() {
      return {
        languages: [],
        locales: [],
        inputLanguage: null,
        inputLocale: null,
      };
    },

    watch: {
      locale() {
        this.setupLanguageTags();
      },

      language() {
        this.setupLanguageTags();
      },
    },

    created() {
      this.languages = [].concat(i18n.getSupportedLanguages());
      this.locales = [].concat(i18n.getSupportedLocales());
      this.setupLanguageTags();
    },

    methods: {
      setupLanguageTags() {
        this.inputLanguage = i18n.localeIdToLanguageTag(this.language);
        this.inputLocale = i18n.localeIdToLanguageTag(this.locale);
      },

      async updateLanguage() {
        // todo #6: loading indicator while locale/language is loaded and applied
        await this.$emit('update:language', i18n.languageTagToLocaleId(this.inputLanguage));
        await i18n.setLocaleFromProfile({
          locale: this.locale,
          language: this.language,
        });
        this.locales = [].concat(i18n.getSupportedLocales());
      },

      async updateLocale() {
        await this.$emit('update:locale', i18n.languageTagToLocaleId(this.inputLocale));
        await i18n.setLocaleFromProfile({
          locale: this.locale,
          language: this.language,
        });
      },
    },
  };
</script>

<style lang="scss">


</style>
