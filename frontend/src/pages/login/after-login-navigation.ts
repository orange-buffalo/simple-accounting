import { useWorkspaces } from '@/services/workspaces';
import { DASHBOARD_ROUTE_NAME } from '@/pages/dashboard/Dashboard.vue';
import { ACCOUNT_SETUP_ROUTE_NAME } from '@/pages/account-setup/AccountSetupPage.vue';
import { USERS_OVERVIEW_ROUTE_NAME } from '@/pages/admin/users/UsersOverview.vue';
import { setLocaleFromProfile } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { useAuth } from '@/services/api';
import { useLastView } from '@/services/use-last-view';
import { graphql } from '@/services/api/gql';
import { useLazyQuery } from '@/services/api/use-gql-api';
import { setSharedWorkspaceTimeZone } from '@/services/time-zone';

/**
 * Applies the profile of the freshly authenticated user and takes them to the landing page.
 * Must be called during component setup, as it relies on Vue composables.
 */
export function useAfterLoginNavigation() {
  const { navigateByViewName } = useNavigation();
  const { isAdmin } = useAuth();

  const fetchUserProfile = useLazyQuery(graphql(/* GraphQL */ `
    query userProfileLogin {
      userProfile {
        i18n {
          language
          locale
          timeZone
        }
      }
    }
  `), 'userProfile');

  const onUserLogin = async () => {
    const hasAnyWorkspaces = await useWorkspaces()
      .loadWorkspaces();
    if (hasAnyWorkspaces) {
      const { lastView } = useLastView();
      if (lastView) {
        await navigateByViewName(lastView);
      } else {
        await navigateByViewName(DASHBOARD_ROUTE_NAME);
      }
    } else {
      await navigateByViewName(ACCOUNT_SETUP_ROUTE_NAME);
    }
  };

  return async () => {
    const profile = await fetchUserProfile({});
    setSharedWorkspaceTimeZone(false);
    await setLocaleFromProfile(profile.i18n.locale, profile.i18n.language, profile.i18n.timeZone);

    if (isAdmin()) {
      await navigateByViewName(USERS_OVERVIEW_ROUTE_NAME);
    } else {
      await onUserLogin();
    }
  };
}
