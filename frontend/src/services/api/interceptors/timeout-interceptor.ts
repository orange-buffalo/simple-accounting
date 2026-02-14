import { getGlobalRequestTimeout } from '@/services/api';
import type { Middleware, RequestContext } from '@/services/api/generated';

export const requestTimeoutInterceptor: Middleware = {
  async pre(context: RequestContext) {
    if (!context.init.signal) {
      return {
        ...context,
        init: {
          ...context.init,
          signal: AbortSignal.timeout(getGlobalRequestTimeout()),
        },
      };
    }
    return context;
  },
};
