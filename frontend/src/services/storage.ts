const prefix = 'simple-accounting.';

class StorageAccessor<T> {
  private readonly key: string;

  private value: T | null;

  constructor(key: string) {
    this.key = key;
    this.value = null;
  }

  set(value: T): void {
    this.value = value;
    localStorage.setItem(
      prefix + this.key,
      JSON.stringify({
        data: value,
      }),
    );
  }

  clear(): void {
    this.value = null;
    localStorage.removeItem(prefix + this.key);
  }

  getOrNull(): T | null {
    if (this.value == null) {
      const storageValueJson = localStorage.getItem(prefix + this.key);
      if (storageValueJson) {
        try {
          const storageValue = JSON.parse(storageValueJson);
          if (storageValue.data) {
            this.value = storageValue.data;
          }
        } catch (_) {
          // no op
        }
      }
    }
    return this.value || null;
  }

  get(): T {
    const maybeValue = this.getOrNull();
    if (maybeValue) return maybeValue;
    throw new Error(`Value for ${this.key} is not found in storage`);
  }

  getOrDefault(defaultValue: T): T {
    return this.getOrNull() || defaultValue;
  }
}

export function useStorage<T>(key: string): StorageAccessor<T> {
  return new StorageAccessor<T>(key);
}
