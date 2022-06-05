import httpMock from 'xhr-mock';

describe('api service', () => {
  let api;

  beforeEach(() => {
    jest.resetModules();
    httpMock.setup();

    ({ api } = require('@/services/api-legacy'));
  });

  afterEach(() => {
    httpMock.reset();
  });

  // todo #205: convert to tests ensuring requests population / response translations
  it('invokes page request without filters', async () => {
    expect.assertions(2);

    const response = {
      pageNumber: 1,
      pageSize: 10,
      totalElements: 1,
      data: [{
        prop: 'value',
      }],
    };

    httpMock.get(/api\/entities?.*/, (req, res) => {
      expect(req.url().query)
        .toEqual({
          limit: '10',
          page: '1',
        });
      return res.status(200)
        .body(response);
    });

    const page = await api.pageRequest('/entities')
      .getPage();

    expect(page)
      .toStrictEqual(response);
  });

  it('should support eq filter', async () => {
    expect.assertions(1);

    httpMock.get(/api\/entities?.*/, (req, res) => {
      expect(req.url().query)
        .toEqual({
          limit: '10',
          page: '1',
          'property[eq]': 'filterValue',
        });
      return res.status(200)
        .body({});
    });

    await api.pageRequest('/entities')
      .eqFilter('property', 'filterValue')
      .getPage();
  });

  it('should stringify values for eq filter', async () => {
    expect.assertions(1);

    httpMock.get(/api\/entities?.*/, (req, res) => {
      expect(req.url().query)
        .toEqual({
          limit: '10',
          page: '1',
          'property[eq]': '42',
        });
      return res.status(200)
        .body({});
    });

    await api.pageRequest('/entities')
      .eqFilter('property', 42)
      .getPage();
  });

  it('should convert arrays to string for eq filter', async () => {
    expect.assertions(1);

    httpMock.get(/api\/entities?.*/, (req, res) => {
      expect(req.url().query)
        .toEqual({
          limit: '10',
          page: '1',
          'property[eq]': '42,43',
        });
      return res.status(200)
        .body({});
    });

    await api.pageRequest('/entities')
      .eqFilter('property', [42, 43])
      .getPage();
  });

  it('should support in filter', async () => {
    expect.assertions(1);

    httpMock.get(/api\/entities?.*/, (req, res) => {
      expect(req.url().query)
        .toEqual({
          limit: '10',
          page: '1',
          'property[in]': '42,43',
        });
      return res.status(200)
        .body({});
    });

    await api.pageRequest('/entities')
      .inFilter('property', [42, 43])
      .getPage();
  });
});
