import { describe, expect, it } from 'vitest';
import { detailedIconNames, iconByName, iconNames } from '@/icons';

describe('icons', () => {
  it('should provide a simple icon for every detailed icon', () => {
    expect(iconNames()).toEqual(expect.arrayContaining(detailedIconNames()));
  });

  it('should provide the detailed icon when it is available', () => {
    expect(iconByName('dashboard', 'detailed')).not.toBe(iconByName('dashboard'));
  });

  it('should fall back to the simple icon when detailed variant is not available', () => {
    expect(detailedIconNames()).not.toContain('menu');
    expect(iconByName('menu', 'detailed')).toBe(iconByName('menu'));
  });

  it('should fail on unknown icon', () => {
    expect(() => iconByName('unknown-icon')).toThrow('unknown-icon is not known');
  });
});
