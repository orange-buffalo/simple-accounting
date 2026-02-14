import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useStorage } from '@/services/storage';

describe('Storage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should return null when nothing in storage', () => {
    const storageAccess = useStorage<string>('myKey');

    expect(storageAccess).toBeDefined();
    expect(storageAccess.getOrNull()).toBeNull();
  });

  it('should return the value from storage', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      expect(key).toBe('simple-accounting.myKey');
      return JSON.stringify({
        data: 'persistedValue',
      });
    });

    const storageAccess = useStorage<string>('myKey');

    expect(storageAccess).toBeDefined();
    expect(storageAccess.getOrNull()).toBe('persistedValue');
  });

  it('should save the value to storage', () => {
    const storageSpy = vi.spyOn(Storage.prototype, 'setItem');

    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess).toBeDefined();
    storageAccess.set('newValue');

    expect(storageSpy).toBeCalledWith(
      'simple-accounting.myKey',
      JSON.stringify({
        data: 'newValue',
      }),
    );
  });

  it('should return default value when nothing in storage', () => {
    const storageAccess = useStorage<string>('myKey');

    expect(storageAccess).toBeDefined();
    expect(storageAccess.getOrDefault('default')).toBe('default');
  });

  it('should remove value from storage', () => {
    const storageSpy = vi.spyOn(Storage.prototype, 'removeItem');

    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess).toBeDefined();
    storageAccess.clear();

    expect(storageSpy).toBeCalledWith('simple-accounting.myKey');
  });

  it('should return the value if present', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      expect(key).toBe('simple-accounting.myKey');
      return JSON.stringify({
        data: 'persistedValue',
      });
    });

    const storageAccess = useStorage<string>('myKey');

    expect(storageAccess).toBeDefined();
    expect(storageAccess.get()).toBe('persistedValue');
  });

  it('should fai if not present', () => {
    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess).toBeDefined();
    expect(() => storageAccess.get()).toThrow('Value for myKey is not found in storage');
  });

  afterEach(() => {
    vi.resetAllMocks();
  });
});
