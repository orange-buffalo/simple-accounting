import { AxiosResponse } from 'axios';
import { ApiPage } from '@/services/api/api-types';
import { Ref } from '@vue/composition-api';

export function apiDateString(date: Date) {
  return `${date.getFullYear()}-${
    (`0${date.getMonth() + 1}`).slice(-2)}-${
    (`0${date.getDate()}`).slice(-2)}`;
}

export function eagerPagination() {
  return {
    pageSize: 100,
  };
}

export function consumePageInto<T>(ref: Ref<Array<T>>) {
  return (response: AxiosResponse<ApiPage<T>>) => {
    // eslint-disable-next-line no-param-reassign
    ref.value = response.data.data;
  };
}
