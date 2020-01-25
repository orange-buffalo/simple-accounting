// eslint-disable-next-line import/prefer-default-export
export function findByIdOrEmpty(list, targetItemId) {
  const result = list
    .find(it => (it.id === targetItemId) || (it.id == null && targetItemId == null));
  return result || {};
}
