import Lockr from 'lockr';

Lockr.prefix = 'simple-accounting.';

/**
 * @deprecated
 */
export const lockr = Lockr;

export const app = {
  init({
    vue,
    store,
    router,
    i18n,
  }) {
    Object.assign(this, {
      vue,
      store,
      router,
      i18n,
    });
  },
};
