import type { SimpleAccountingInitializer } from '@/setup/setup-app';
import { removeLoader } from '@/setup/loader';

async function resolveDeferredAndSetupApp(setupAppDeferred: Promise<{ default: SimpleAccountingInitializer }>) {
  const {
    default: {
      app,
      setupApp,
      mountApp,
    },
  } = await setupAppDeferred;
  setupApp();
  return ({
    app,
    mountApp,
  });
}

export async function bootstrapApp() {
  const setupAppDeferred = import('@/setup/setup-app');

  const { useAuth } = await import('@/services/api');
  const { tryAutoLogin } = useAuth();
  const targetRoute = window.location.pathname;

  // TODO
  if (targetRoute.includes('login-by-link')) {
    // const { app, mountApp } = await resolveDeferredAndSetupApp(setupAppDeferred);
    // await app.i18n.setLocaleFromBrowser();
    // if (app.router.currentRoute.path !== targetRoute) {
    //   await app.router.push(targetRoute);
    // }
    // mountApp();
  } else if (await tryAutoLogin()) {
    // const { app, mountApp } = await resolveDeferredAndSetupApp(setupAppDeferred);
    //
    // const { userApi } = await import(/* webpackChunkName: "user-api" */ '@/services/user-api');
    // const profile = await userApi.getProfile();
    // await app.i18n.setLocaleFromProfile(profile.i18n);
    //
    // if (app.router.currentRoute.path !== targetRoute) {
    //   await app.router.push(targetRoute);
    // }
    //
    // const { initWorkspace } = await import(/* webpackChunkName: "workspaces" */ '@/services/workspaces');
    // await initWorkspace();
    //
    // mountApp();
  } else {
    // const { app, mountApp } = await resolveDeferredAndSetupApp(setupAppDeferred);
    // await app.i18n.setLocaleFromBrowser();
    // if (app.router.currentRoute.path !== '/login') {
    //   await app.router.push({ name: 'login' });
    // }
    // mountApp();
  }

  // removeLoader();
}
