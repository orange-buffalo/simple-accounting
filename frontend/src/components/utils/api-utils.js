import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
import { safeAssign } from '@/components/utils/utils';
import { api } from '@/services/api';

export function useApiCrud({
  apiEntityPath,
  entity,
  loading,
  withLoading,
  withLoadingProducer,
}) {
  const { currentWorkspaceApiUrl } = useCurrentWorkspace();

  const saveEntity = (entityRequest) => withLoading(async () => {
    if (entity.id) {
      await api.put(currentWorkspaceApiUrl(`${apiEntityPath}/${entity.id}`), removeId(entityRequest));
    } else {
      await api.post(currentWorkspaceApiUrl(apiEntityPath), removeId(entityRequest));
    }
  });

  const loadEntity = (entityConsumer) => withLoading(async () => {
    if (entity.id) {
      const entityResponse = await api.get(currentWorkspaceApiUrl(`${apiEntityPath}/${entity.id}`));
      if (entityConsumer == null) {
        safeAssign(entity, entityResponse.data);
      } else {
        entityConsumer(entityResponse.data);
      }
    }
  });

  return {
    saveEntity,
    loadEntity,
    loading,
    withLoading,
    withLoadingProducer,
  };
}

export function removeId(entity) {
  const {
    id,
    ...entityWithoutId
  } = entity;
  return entityWithoutId;
}
