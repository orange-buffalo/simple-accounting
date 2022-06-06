type Listener = (data: unknown) => void;

class Event {
  listeners: Array<Listener>;

  constructor() {
    this.listeners = [];
  }

  emit(data: unknown = null) {
    this.listeners.forEach((listener) => listener(data));
  }

  subscribe(listener: Listener) {
    this.listeners.push(listener);
  }

  unsubscribe(listener: Listener) {
    this.listeners = this.listeners.filter((registeredListener) => registeredListener !== listener);
  }
}

export const LOGIN_REQUIRED_EVENT = new Event();
export const SUCCESSFUL_LOGIN_EVENT = new Event();

export const LOADING_STARTED_EVENT = new Event();
export const LOADING_FINISHED_EVENT = new Event();

export const API_FATAL_ERROR_EVENT = new Event();

export const WORKSPACE_CHANGED_EVENT = new Event();
