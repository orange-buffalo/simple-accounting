import type { App } from 'vue';
import type { Router } from 'vue-router';
import type { I18nService } from '@/services/i18n';

export class AppServices {
  vue!: App;

  router!: Router;

  i18n!: I18nService;

  init(
    vue: App,
    router: Router,
    i18n: I18nService,
  ) {
    this.vue = vue;
    this.router = router;
    this.i18n = i18n;
  }
}

export const app = new AppServices();
