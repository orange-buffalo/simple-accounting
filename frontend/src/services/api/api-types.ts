export interface ApiPage<T> {
  pageNumber: number,
  totalElements: number,
  pageSize: number,
  data: Array<T>,
}
