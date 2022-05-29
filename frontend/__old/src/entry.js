// import '@/styles/loader.scss';
//
// const appDiv = document.getElementById('simple-accounting');
// appDiv.style.display = 'none';
//
// const loaderDiv = document.createElement('div');
// loaderDiv.className = 'app-loader-screen';
//
// loaderDiv.innerHTML += `
// <div class="app-loader-container">
//   <h1>simple-accounting</h1>
//   <div class="app-loader">
//   </div>
// </div>
// `;
//
// document.body.appendChild(loaderDiv);

// async function resolveDeferredAndSetupApp(setupAppDeferred) {
//   const {
//     default: {
//       app,
//       setupApp,
//       mountApp,
//     },
//   } = await setupAppDeferred;
//   setupApp();
//   return ({
//     app,
//     mountApp,
//   });
// }

async function initApp() {
  const setupAppDeferred = import(/* webpackPreload: true, webpackChunkName: "setup-app" */ '@/setup/setup-app');
  const { useAuth } = await import(/* webpackPreload: true, webpackChunkName: "api-services" */ '@/services/api');
  const { tryAutoLogin } = useAuth();
  const targetRoute = window.location.pathname;
  if (targetRoute.includes('login-by-link')) {
    const { app, mountApp } = await resolveDeferredAndSetupApp(setupAppDeferred);
    await app.i18n.setLocaleFromBrowser();
    if (app.router.currentRoute.path !== targetRoute) {
      await app.router.push(targetRoute);
    }
    mountApp();
  } else if (await tryAutoLogin()) {
    const { app, mountApp } = await resolveDeferredAndSetupApp(setupAppDeferred);

    const { userApi } = await import(/* webpackChunkName: "user-api" */ '@/services/user-api');
    const profile = await userApi.getProfile();
    await app.i18n.setLocaleFromProfile(profile.i18n);

    if (app.router.currentRoute.path !== targetRoute) {
      await app.router.push(targetRoute);
    }

    const { initWorkspace } = await import(/* webpackChunkName: "workspaces" */ '@/services/workspaces');
    await initWorkspace();

    mountApp();
  } else {
    const { app, mountApp } = await resolveDeferredAndSetupApp(setupAppDeferred);
    await app.i18n.setLocaleFromBrowser();
    if (app.router.currentRoute.path !== '/login') {
      await app.router.push({ name: 'login' });
    }
    mountApp();
  }

  loaderDiv.setAttribute('style', 'opacity: 0');
  setTimeout(() => loaderDiv.remove(), 500);
}

// noinspection JSIgnoredPromiseFromCall
initApp();
