import type { UnwrapRef } from 'vue';
import { ref, watch } from 'vue';
import type { HasOptionalId } from '@/services/api';
import { useCurrentWorkspace } from '@/services/workspaces';

// TODO: remove if not used
export function findByIdOrEmpty<T extends HasOptionalId>(list: T[], targetItemId?: number): T | undefined {
  return list
    .find((it) => (it.id === targetItemId) || (!it.id && !targetItemId));
}

export function useValueLoadedByCurrentWorkspaceAndProp<T, P>(
  propGetter: () => P,
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
