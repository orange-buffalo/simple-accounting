import type { App } from 'vue';
import type { Router } from 'vue-router';

export class AppServices {
  vue!: App;

  router!: Router;

  init(
    vue: App,
    router: Router,
  ) {
    this.vue = vue;
    this.router = router;
  }
}

export const app = new AppServices();
