export default function pageRequest(uri) {
  let limit = 10;
  let page = 1;
  let customConfig = {};
  let filters = {};

  const addFilter = (property, value, operator) => {
    if (value != null) {
      const filter = {};

      filter[property] = {};
      filter[property][operator] = String(value);

      filters = { ...filters, ...filter };
    }
  };

  const api = this;

  return {
    limit(value) {
      limit = value;
      return this;
    },

    eager() {
      // we still want to limit the data amount with some reasonable number
      return this.limit(100);
    },

    page(value) {
      page = value;
      return this;
    },

    config(value) {
      customConfig = value;
      return this;
    },

    eqFilter(property, value) {
      addFilter(property, value, 'eq');
      return this;
    },

    inFilter(property, value) {
      addFilter(property, value, 'in');
      return this;
    },

    get() {
      const params = {
        limit,
        page,
        ...filters,
      };
      const config = { params, ...customConfig };
      return api.get(uri, config);
    },

    getPage() {
      return this.get()
        .then((response) => response.data);
    },

    getPageData() {
      return this.getPage()
        .then((pageResponse) => pageResponse.data);
    },
  };
}
