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

  // todo
  const { mountApp } = await resolveDeferredAndSetupApp(setupAppDeferred);
  mountApp();

  removeLoader();
}
