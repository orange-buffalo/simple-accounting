import { useWorkspaces } from '@/services/workspaces';
import { setLocaleFromProfile } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { useAuth } from '@/services/api';
import { useLastView } from '@/services/use-last-view';
import { graphql } from '@/services/api/gql';
import { useLazyQuery } from '@/services/api/use-gql-api';

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
        await navigateByViewName('dashboard');
      }
    } else {
      await navigateByViewName('account-setup');
    }
  };

  return async () => {
    const profile = await fetchUserProfile({});
    await setLocaleFromProfile(profile.i18n.locale, profile.i18n.language);

    if (isAdmin()) {
      await navigateByViewName('users-overview');
    } else {
      await onUserLogin();
    }
  };
}
