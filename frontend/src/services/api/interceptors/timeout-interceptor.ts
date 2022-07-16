/* eslint-disable no-param-reassign */
import type {
  Middleware,
  RequestContext,
} from '@/services/api/generated';
import type { RequestMetadata } from '@/services/api/api-client';

const DEFAULT_TIMEOUT = 10000;

export const requestTimeoutInterceptor: Middleware<RequestMetadata> = {
  async pre(context: RequestContext<RequestMetadata>) {
    const timeoutMs = context.metadata?.requestTimeoutMs || DEFAULT_TIMEOUT;
    let finalSignal: AbortSignal;
    let timerHandler: ReturnType<typeof setTimeout> | null = null;
    if (context.init.signal) {
      if (context.init.signal.aborted) {
        finalSignal = context.init.signal;
      } else {
        const mergedAbortController = new AbortController();
        finalSignal = mergedAbortController.signal;
        context.init.signal.onabort = () => {
          mergedAbortController.abort('Request was aborted');
        };
        timerHandler = setTimeout(() => {
          mergedAbortController.abort('Request timeout reached');
        }, timeoutMs);
      }
    } else {
      const timeoutAbortController = new AbortController();
      timerHandler = setTimeout(() => {
        timeoutAbortController.abort('Request timeout reached');
      }, timeoutMs);
      finalSignal = timeoutAbortController.signal;
    }

    if (timerHandler) {
      if (!context.metadata) {
        context.metadata = {};
      }
      context.metadata.requestTimeoutHandler = timerHandler;
    }

    return {
      ...context,
      init: {
        ...context.init,
        signal: finalSignal,
      },
    };
  },

  async post(context: RequestContext<RequestMetadata>) {
    if (!context.metadata) return;
    const { requestTimeoutHandler } = context.metadata;
    clearTimeout(requestTimeoutHandler);
  },
};
