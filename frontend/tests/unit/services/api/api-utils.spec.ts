import {
  apiDateString, ApiPage, consumeAllPages, SimpleAccountingClient,
} from '@/services/api';
import httpMock from 'xhr-mock';

describe('apiDateString', () => {
  it('should convert date without zeros', () => {
    expect(apiDateString(new Date('2030-10-13T00:00:00')))
      .toBe('2030-10-13');
  });

  it('should convert date with zeros', () => {
    expect(apiDateString(new Date('2030-02-06T00:00:00')))
      .toBe('2030-02-06');
  });
});

interface FakeEntity {
  name: string,
}

describe('consumeAllPages', () => {
  let apiClient: SimpleAccountingClient;

  beforeEach(() => {
    jest.resetModules();
    httpMock.setup();
    ({ apiClient } = require('@/services/api'));
  });

  afterEach(() => {
    httpMock.reset();
  });

  it('invokes page request with proper page parameters', async () => {
    expect.assertions(1);

    httpMock.get('/api-call?pageNumber=1&pageSize=100', (req, res) => res.status(200)
      .body({
        pageNumber: 1,
        pageSize: 100,
        totalElements: 0,
        data: [],
      } as ApiPage<FakeEntity>));

    const entities = await consumeAllPages<FakeEntity>((pageRequest) => apiClient.get('/api-call', {
      params: pageRequest,
    }));
    expect(entities)
      .toHaveLength(0);
  });

  it('returns data for single page', async () => {
    expect.assertions(1);

    const data = [{
      name: 'name1',
    }, {
      name: 'name2',
    }];

    httpMock.get('/api-call?pageNumber=1&pageSize=100', (req, res) => res.status(200)
      .body({
        pageNumber: 1,
        pageSize: 100,
        totalElements: 2,
        data,
      } as ApiPage<FakeEntity>));

    const entities = await consumeAllPages<FakeEntity>((pageRequest) => apiClient.get('/api-call', {
      params: pageRequest,
    }));
    expect(entities)
      .toStrictEqual(data);
  });

  it('merges multiple pages', async () => {
    expect.assertions(1);

    httpMock.get('/api-call?pageNumber=1&pageSize=100', (req, res) => res.status(200)
      .body({
        pageNumber: 1,
        pageSize: 100,
        totalElements: 200,
        data: [{
          name: 'name1',
        }, {
          name: 'name2',
        }],
      } as ApiPage<FakeEntity>));

    httpMock.get('/api-call?pageNumber=2&pageSize=100', (req, res) => res.status(200)
      .body({
        pageNumber: 2,
        pageSize: 100,
        totalElements: 200,
        data: [{
          name: 'name3',
        }],
      } as ApiPage<FakeEntity>));

    const entities = await consumeAllPages<FakeEntity>((pageRequest) => apiClient.get('/api-call', {
      params: pageRequest,
    }));
    expect(entities)
      .toStrictEqual([{
        name: 'name1',
      }, {
        name: 'name2',
      }, {
        name: 'name3',
      }]);
  });
});
