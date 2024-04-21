import { removeLoader } from '@/setup/loader';
import { ANONYMOUS_PAGES_PATH_PREFIXES } from '@/setup/setup-router.ts';

export async function bootstrapApp() {
  const { useAuth } = await import('@/services/api');
  const { tryAutoLogin } = useAuth();
  const targetRoute = window.location.pathname;

  const {
    setupApp,
    mountApp,
    setLocaleFromBrowser,
    setLocaleFromProfile,
    router,
  } = await import('@/setup/setup-app');

  setupApp();

  const isAnonymousPage = ANONYMOUS_PAGES_PATH_PREFIXES
    .some((prefix) => targetRoute.startsWith(prefix));
  // todo #117: remove from the list of explicit checks
  if (targetRoute.includes('login-by-link') || isAnonymousPage) {
    await setLocaleFromBrowser();

    if (router().currentRoute.value.path !== targetRoute) {
      await router()
        .push(targetRoute);
    }
  } else if (await tryAutoLogin()) {
    const { profileApi } = await import('@/services/api');
    const profile = await profileApi.getProfile();
    await setLocaleFromProfile(profile.i18n.locale, profile.i18n.language);

    if (router().currentRoute.value.path !== targetRoute) {
      await router()
        .push(targetRoute);
    }

    const { initWorkspace } = await import('@/services/workspaces');
    await initWorkspace();
  } else {
    await setLocaleFromBrowser();

    if (router().currentRoute.value.path !== '/login') {
      await router()
        .push({ name: 'login' });
    }
  }

  mountApp();
  removeLoader();
}
