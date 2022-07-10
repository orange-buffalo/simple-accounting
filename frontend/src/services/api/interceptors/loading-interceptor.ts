import type { Middleware } from '@/services/api/generated';
import type { RequestMetadata } from '@/services/api/api-client';
import { LOADING_FINISHED_EVENT, LOADING_STARTED_EVENT } from '@/services/events';

function emitLoadingFinishedEvent() {
  LOADING_FINISHED_EVENT.emit();
}

export const loadingEventsInterceptor: Middleware<RequestMetadata> = {
  async pre() {
    LOADING_STARTED_EVENT.emit();
  },
  async post() {
    emitLoadingFinishedEvent();
  },
  async onError() {
    emitLoadingFinishedEvent();
  },
};
