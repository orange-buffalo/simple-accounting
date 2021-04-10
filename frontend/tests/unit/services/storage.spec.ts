import { useStorage } from '@/services/storage';

describe('Storage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should return null when nothing in storage', () => {
    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess)
      .toBeDefined();
    expect(storageAccess.getOrNull())
      .toBeNull();
  });

  it('should return the value from storage', () => {
    // eslint-disable-next-line no-underscore-dangle
    localStorage.__STORE__['simple-accounting.myKey'] = JSON.stringify({
      data: 'persistedValue',
    });
    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess)
      .toBeDefined();
    expect(storageAccess.getOrNull())
      .toBe('persistedValue');
  });

  it('should save the value to storage', () => {
    // eslint-disable-next-line no-underscore-dangle
    localStorage.__STORE__['simple-accounting.myKey'] = {
      data: 'persistedValue',
    };
    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess)
      .toBeDefined();
    storageAccess.set('newValue');
    // eslint-disable-next-line no-underscore-dangle
    expect(localStorage.__STORE__['simple-accounting.myKey'])
      .toBe(JSON.stringify({
        data: 'newValue',
      }));
  });

  it('should return default value when nothing in storage', () => {
    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess)
      .toBeDefined();
    expect(storageAccess.getOrDefault('default'))
      .toBe('default');
  });

  it('should remove value from storage', () => {
    // eslint-disable-next-line no-underscore-dangle
    localStorage.__STORE__['simple-accounting.myKey'] = {
      data: 'persistedValue',
    };
    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess)
      .toBeDefined();
    storageAccess.clear();
    // eslint-disable-next-line no-underscore-dangle
    expect(Object.keys(localStorage.__STORE__).length)
      .toBe(0);
  });

  it('should return the value if present', () => {
    // eslint-disable-next-line no-underscore-dangle
    localStorage.__STORE__['simple-accounting.myKey'] = JSON.stringify({
      data: 'persistedValue',
    });
    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess)
      .toBeDefined();
    expect(storageAccess.get())
      .toBe('persistedValue');
  });

  it('should fai if not present', () => {
    const storageAccess = useStorage<string>('myKey');
    expect(storageAccess)
      .toBeDefined();
    expect(() => storageAccess.get())
      .toThrow('Value for myKey is not found in storage');
  });
});
