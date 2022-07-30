import fetchMockClient from 'fetch-mock/esm/client';
import type { MockOptionsMethodGet } from 'fetch-mock';
import { DEFAULT_STORIES_WORKSPACE_ID } from '@/__storybook__/decorators/stories-workspace-mocks';

export const fetchMock = fetchMockClient;

export const defaultWorkspacePath = (path: string) => `/api/workspaces/${DEFAULT_STORIES_WORKSPACE_ID}${path}`;

export const neverEndingGetRequest: MockOptionsMethodGet = {
  delay: 999999,
};
