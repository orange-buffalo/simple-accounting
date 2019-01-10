import Lockr from 'lockr'
import api from '@/services/api'

export const setupApp = function (store) {
  store.dispatch('app/loadCurrencies')
  return store.dispatch('workspaces/loadWorkspaces')
}

Lockr.prefix = 'simple-accounting.'
export const lockr = Lockr

export const loadDocuments = async function (documents, documentsIds, workspaceId) {
  if (documents.length === 0 && documentsIds && documentsIds.length) {
    return await api.pageRequest(`/user/workspaces/${workspaceId}/documents`)
        .eager()
        .eqFilter("id", documentsIds)
        .getPageData()
  }
  return documents
}