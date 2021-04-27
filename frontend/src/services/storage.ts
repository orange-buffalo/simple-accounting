import Lockr from 'lockr';

Lockr.prefix = 'simple-accounting.';

class StorageAccessor<T> {
  private readonly key: string;

  private value: T | null;

  constructor(key: string) {
    this.key = key;
    this.value = null;
  }

  set(value: T): void {
    this.value = value;
    Lockr.set(this.key, value);
  }

  clear(): void {
    this.value = null;
    Lockr.rm(this.key);
  }

  getOrNull(): T | null {
    if (this.value == null) {
      this.value = Lockr.get(this.key);
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
