class Event {
  constructor() {
    this.listeners = [];
  }

  emit(data) {
    this.listeners.forEach(listener => listener(data));
  }

  subscribe(listener) {
    this.listeners.push(listener);
  }

  unsubscribe(listener) {
    this.listeners = this.listeners.filter(registeredListener => registeredListener !== listener);
  }
}

export const LOGIN_REQUIRED_EVENT = new Event();
export const SUCCESSFUL_LOGIN_EVENT = new Event();
