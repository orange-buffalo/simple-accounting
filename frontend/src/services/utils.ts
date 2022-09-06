import type { UnwrapRef, Ref, ComputedRef } from 'vue';
import { computed, ref, watch } from 'vue';
import type { HasOptionalId } from '@/services/api';
import { useCurrentWorkspace } from '@/services/workspaces';

export function findByIdOrEmpty<T extends HasOptionalId>(list: T[], targetItemId?: number): T | undefined {
  return list
    .find((it) => (it.id === targetItemId) || (!it.id && !targetItemId));
}

export function useValueLoadedByCurrentWorkspaceAndProp<T, P>(
  propGetter: () => P | null | undefined,
  valueLoader: (propValue: P, currentWorkspaceId: number) => Promise<UnwrapRef<T>>,
) {
  const value = ref<T | null>(null);
  const loading = ref(true);
  const { currentWorkspaceId } = useCurrentWorkspace();

  watch(propGetter, async (propValue) => {
    if (!propValue) return;
    loading.value = true;
    value.value = await valueLoader(propValue, currentWorkspaceId);
    loading.value = false;
  }, { immediate: true });

  return {
    value,
    loading,
  };
}

export function useValueLoadedByCurrentWorkspace<T>(
  valueLoader: (currentWorkspaceId: number) => Promise<UnwrapRef<T>>,
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
