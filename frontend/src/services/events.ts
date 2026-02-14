import type { WorkspaceDto } from '@/services/api';

export type Listener<T> = (data: T) => void;

export interface Event<T> {
  emit(data: T): void;

  subscribe(listener: Listener<T>): void;

  unsubscribe(listener: Listener<T>): void;
}

class EventImpl<T> implements Event<T> {
  listeners: Array<Listener<T>>;

  constructor() {
    this.listeners = [];
  }

  emit(data: T) {
    this.listeners.forEach((listener) => {
      listener(data);
    });
  }

  subscribe(listener: Listener<T>) {
    this.listeners.push(listener);
  }

  unsubscribe(listener: Listener<T>) {
    this.listeners = this.listeners.filter((registeredListener) => registeredListener !== listener);
  }
}

export const LOGIN_REQUIRED_EVENT: Event<void> = new EventImpl();
export const SUCCESSFUL_LOGIN_EVENT: Event<void> = new EventImpl();

export const LOADING_STARTED_EVENT: Event<void> = new EventImpl();
export const LOADING_FINISHED_EVENT: Event<void> = new EventImpl();

export const WORKSPACE_CHANGED_EVENT: Event<WorkspaceDto> = new EventImpl();
