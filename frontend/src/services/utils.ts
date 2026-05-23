import type { UnwrapRef, Ref, ComputedRef } from 'vue';
import { computed, ref } from 'vue';
import { useCurrentWorkspace } from '@/services/workspaces';

export function useValueLoadedByCurrentWorkspace<T>(
  valueLoader: (currentWorkspaceId: string) => Promise<UnwrapRef<T>>,
) {
  const value = ref<T | null>(null);
  const loading = ref(true);
  const { currentWorkspaceId } = useCurrentWorkspace();

  const loadValue = async () => {
    loading.value = true;
    value.value = await valueLoader(currentWorkspaceId);
    loading.value = false;
  };

  // noinspection JSIgnoredPromiseFromCall
  loadValue();

  return {
    value,
    loading,
  };
}

export function wrapNullable<T extends object>(target: Ref<T | null>): ComputedRef<Partial<T>> {
  return computed(() => target.value || {});
}

export function ensureDefined<T>(value: T | undefined | null): T {
  if (value === null || value === undefined) {
    throw new Error('Value undefined');
  }
  return value;
}
