/* eslint-disable no-param-reassign */
import type {
  Middleware,
  RequestContext,
} from '@/services/api/generated';
import type { RequestMetadata } from '@/services/api/api-client';
import { ApiTimeoutError } from '@/services/api/api-errors.ts';

const DEFAULT_TIMEOUT_MS = 10000;

let currentGlobalTimeoutMs = DEFAULT_TIMEOUT_MS;

/**
 * Updates the global request timeout. Only intended for one-time global setup, e.g. in tests.
 */
export function setGlobalRequestTimeout(requestTimeoutMs: number) {
  currentGlobalTimeoutMs = requestTimeoutMs;
}

export const requestTimeoutInterceptor: Middleware<RequestMetadata> = {
  async pre(context: RequestContext<RequestMetadata>) {
    const timeoutMs = context.metadata?.requestTimeoutMs || currentGlobalTimeoutMs;
    let finalSignal: AbortSignal;
    let timerHandler: ReturnType<typeof setTimeout> | null = null;
    const timerFactory = (abortController: AbortController) => setTimeout(() => {
      abortController.abort(new ApiTimeoutError(`Request timed out (${timeoutMs}ms)`));
    }, timeoutMs);

    if (context.init.signal) {
      if (context.init.signal.aborted) {
        finalSignal = context.init.signal;
      } else {
        const mergedAbortController = new AbortController();
        finalSignal = mergedAbortController.signal;
        const originalSignal = context.init.signal;
        originalSignal.onabort = () => {
          mergedAbortController.abort(originalSignal.reason);
        };
        timerHandler = timerFactory(mergedAbortController);
      }
    } else {
      const timeoutAbortController = new AbortController();
      timerHandler = timerFactory(timeoutAbortController);
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
