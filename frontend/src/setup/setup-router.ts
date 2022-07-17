import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/pages/login/Login.vue';
// import { SUCCESSFUL_LOGIN_EVENT, LOGIN_REQUIRED_EVENT } from '@/services/events';
// import { useLastView } from '@/services/use-last-view';
// import router from './routes-definitions';

// function setupAuthenticationHooks() {
//   const {
//     isLoggedIn,
//     tryAutoLogin,
//   } = useAuth();
//   router.beforeEach(async (to, from, next) => {
//     const { setLastView } = useLastView();
//     if (to.name !== 'login'
//       && to.name !== 'logout'
//       && to.name !== 'login-by-link'
//       && to.name !== 'oauth-callback'
//       && !isLoggedIn()) {
//       if (await tryAutoLogin()) {
//         SUCCESSFUL_LOGIN_EVENT.emit();
//         next();
//       } else {
//         setLastView(to.name);
//         next({ name: 'login' });
//       }
//     } else {
//       next();
//     }
//   });
//
//   LOGIN_REQUIRED_EVENT.subscribe(() => router.push('/login'));
// }

export default function setupRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: '/login',
        name: 'login',
        component: Login,
      },
    ],
  });

  // setupAuthenticationHooks();
  return router;
}
