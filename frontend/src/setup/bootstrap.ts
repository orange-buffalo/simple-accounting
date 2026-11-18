import { removeLoader } from '@/setup/loader';
import { LOGIN_ROUTE_NAME } from '@/pages/login/Login.vue';
import { ANONYMOUS_PAGES_PATH_PREFIXES } from '@/setup/setup-router.ts';
import { useAuth } from '@/services/api';
import { initWorkspace } from '@/services/workspaces.ts';
import { graphql } from '@/services/api/gql';
import { gqlClient } from '@/services/api/gql-api-client.ts';
import { setProfileTimeZone } from '@/services/time-zone';

const userProfileQuery = graphql(/* GraphQL */ `
  query userProfileBootstrap {
    userProfile {
      i18n {
        language
        locale
      }
      timeZone
    }
  }
`);

export async function bootstrapApp() {
  const { tryAutoLogin, isAdmin } = useAuth();
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
  if (isAnonymousPage) {
    setProfileTimeZone(null);
    await setLocaleFromBrowser();

    if (router().currentRoute.value.path !== targetRoute) {
      await router()
        .push(targetRoute);
    }
  } else if (await tryAutoLogin()) {
    const profileData = await gqlClient.query(userProfileQuery, {});
    const { locale, language } = profileData.userProfile.i18n;
    setProfileTimeZone(profileData.userProfile.timeZone);
    await setLocaleFromProfile(locale, language);

    if (router().currentRoute.value.path !== targetRoute) {
      await router()
        .push(targetRoute);
    }

    if (!isAdmin()) {
      await initWorkspace();
    }
  } else {
    setProfileTimeZone(null);
    await setLocaleFromBrowser();

    if (router().currentRoute.value.path !== '/login') {
      await router()
        .push({ name: LOGIN_ROUTE_NAME });
    }
  }

  mountApp();
  removeLoader();
}
