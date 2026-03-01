import { removeLoader } from '@/setup/loader';
import { ANONYMOUS_PAGES_PATH_PREFIXES } from '@/setup/setup-router.ts';
import { useAuth } from '@/services/api';
import { initWorkspace } from '@/services/workspaces.ts';
import { graphql } from '@/services/api/gql';
import { gqlClient } from '@/services/api/gql-api-client.ts';

const userProfileQuery = graphql(/* GraphQL */ `
  query userProfileBootstrap {
    userProfile {
      i18n {
        language
        locale
      }
    }
  }
`);

export async function bootstrapApp() {
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
  if (isAnonymousPage) {
    await setLocaleFromBrowser();

    if (router().currentRoute.value.path !== targetRoute) {
      await router()
        .push(targetRoute);
    }
  } else if (await tryAutoLogin()) {
    const profileData = await gqlClient.query(userProfileQuery, {});
    await setLocaleFromProfile(profileData.userProfile.i18n.locale, profileData.userProfile.i18n.language);

    if (router().currentRoute.value.path !== targetRoute) {
      await router()
        .push(targetRoute);
    }

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
