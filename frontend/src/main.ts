import { createLoader } from '@/setup/loader';

createLoader();

import('@/setup/bootstrap')
  .then(({ bootstrapApp }) => {
    // noinspection JSIgnoredPromiseFromCall
    bootstrapApp();
  });
