import {
  isRef,
  reactive, ref, Ref, watch,
} from '@vue/composition-api';
import axios, { AxiosResponse, CancelTokenSource } from 'axios';
import throttle from 'lodash/throttle';
import { ApiPage, ApiPageRequest } from '@/services/api';
import { AxiosRequestConfig } from 'openapi-client-axios';

export interface PageableItems<D> {
  pageNumber: Ref<number>,
  totalElements: Ref<number>,
  pageSize: Ref<number>,
  data: Ref<D[]>,
  loading: Ref<boolean>,
}

interface PageableItemsReturn<D> {
  items: PageableItems<D>,
  reload: () => Promise<void>,
}

function useLoading() {
  let loadingRequestsCount = 0;
  const loading = ref(true);
  const updateLoading = throttle(
    () => {
      loading.value = loadingRequestsCount > 0;
    },
    200,
    {
      leading: false,
      trailing: true,
    },
  );

  const startLoading = () => {
    loadingRequestsCount += 1;
    updateLoading();
  };

  const stopLoading = () => {
    loadingRequestsCount = Math.max(0, loadingRequestsCount - 1);
    updateLoading();
  };

  return {
    loading,
    startLoading,
    stopLoading,
  };
}

export function usePageableItems<R extends ApiPageRequest, D>(
  requestParameters: R,
  requestExecutor: (request: R, config: AxiosRequestConfig) => Promise<AxiosResponse<ApiPage<D>>>,
): PageableItemsReturn<D> {
  const pageNumber = ref(1);
  const totalElements = ref(0);
  const pageSize = ref(10);
  const data = ref<D[]>([]) as Ref<D[]>;

  // eslint-disable-next-line no-param-reassign
  requestParameters.pageNumber = pageNumber as any;
  // eslint-disable-next-line no-param-reassign
  requestParameters.pageSize = pageSize as any;

  const {
    loading,
    stopLoading,
    startLoading,
  } = useLoading();

  let cancelToken: CancelTokenSource | null;

  const reloadData = throttle(async (updatedParameters: R) => {
    startLoading();

    if (cancelToken) {
      cancelToken.cancel();
    }
    cancelToken = axios.CancelToken.source();

    try {
      const response = await requestExecutor(updatedParameters, {
        cancelToken: cancelToken!.token,
      });
      totalElements.value = response.data.totalElements;
      data.value = response.data.data;

      stopLoading();
    } catch (e) {
      stopLoading();
      if (!axios.isCancel(e)) {
        // todo #72: proper error handling
        throw e;
      }
    }
  }, 300, {
    trailing: true,
    leading: false,
  });

  watch(Object.values(requestParameters).filter((it) => isRef(it)), () => {
    // unwrap refs
    reloadData(reactive(requestParameters) as R);
  }, {
    immediate: true,
    deep: true,
  });

  return {
    items: {
      pageNumber,
      totalElements,
      pageSize,
      data,
      loading,
    },
    reload: () => reloadData(requestParameters) || Promise.resolve(),
  };
}
