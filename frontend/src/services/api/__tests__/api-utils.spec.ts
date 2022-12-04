import {
  describe, test, expect, vi, afterEach,
} from 'vitest';
import fetchMock from 'fetch-mock/esm/client';
import type { ApiPageRequest } from '@/services/api';
import {
  apiDateString, consumeAllPages, consumeApiErrorResponse, profileApi,
} from '@/services/api';

describe('apiDateString', () => {
  test('should convert date without zeros', () => {
    expect(apiDateString(new Date('2030-10-13T00:00:00')))
      .toBe('2030-10-13');
  });

  test('should convert date with zeros', () => {
    expect(apiDateString(new Date('2030-02-06T00:00:00')))
      .toBe('2030-02-06');
  });
});

// TODO #478: enable once fixed
describe.skip('consumeAllPage', () => {
  test('should invoke API only once if total size is less then page size', async () => {
    let executions = 0;
    const mockExecutor = async (pageRequest: ApiPageRequest) => {
      executions += 1;
      expect(executions)
        .eq(1);
      expect(pageRequest.pageNumber)
        .toEqual(1);
      expect(pageRequest.pageSize)
        .toEqual(100);
      return {
        data: ['a'],
        totalElements: 99,
        pageNumber: 1,
        pageSize: 100,
      };
    };

    const requestExecutor = vi.fn(mockExecutor);

    const data = await consumeAllPages(requestExecutor);
    expect(data)
      .to
      .eql(['a']);
  });

  test('should invoke API only once if total size is equal to page size', async () => {
    let executions = 0;
    const mockExecutor = async (pageRequest: ApiPageRequest) => {
      executions += 1;
      expect(executions)
        .eq(1);
      expect(pageRequest.pageNumber)
        .toEqual(1);
      expect(pageRequest.pageSize)
        .toEqual(100);
      return {
        data: ['a'],
        totalElements: 100,
        pageNumber: 1,
        pageSize: 100,
      };
    };

    const requestExecutor = vi.fn(mockExecutor);

    const data = await consumeAllPages(requestExecutor);
    expect(data)
      .to
      .eql(['a']);
  });

  test('should invoke API multiple times and concat results, if total size is greater then page size', async () => {
    let executions = 0;
    const mockExecutor = async (pageRequest: ApiPageRequest) => {
      executions += 1;
      expect(pageRequest.pageNumber)
        .toEqual(executions);
      expect(pageRequest.pageSize)
        .toEqual(100);
      return {
        data: [executions === 1 ? 'a' : 'b'],
        totalElements: 101,
        pageNumber: executions,
        pageSize: 100,
      };
    };

    const requestExecutor = vi.fn(mockExecutor);

    const data = await consumeAllPages(requestExecutor);
    expect(data)
      .to
      .eql(['a', 'b']);
    expect(executions)
      .toEqual(2);
  });
});

// TODO #478: enable once fixed
describe.skip('consumeApiErrorResponse', () => {
  test('should consume API error response', async () => {
    fetchMock.get('/api/profile', {
      status: 409,
      body: {
        responseField: 42,
      },
    });

    interface ErrorResponse {
      responseField: number;
    }

    try {
      await profileApi.getProfile();
      expect(null, 'API call expected to fail')
        .toBeDefined();
    } catch (e) {
      const response = await consumeApiErrorResponse<ErrorResponse>(e);
      expect(response)
        .toEqual({ responseField: 42 });
    }
  });

  test('should consume API error response as undefined on fetch error', async () => {
    fetchMock.get('/api/profile', {
      throws: new Error(),
    });

    try {
      await profileApi.getProfile();
      expect(null, 'API call expected to fail')
        .toBeDefined();
    } catch (e) {
      const response = await consumeApiErrorResponse(e);
      expect(response)
        .toBeUndefined();
    }
  });

  afterEach(() => {
    vi.resetAllMocks();
    fetchMock.restore();
  });
});
