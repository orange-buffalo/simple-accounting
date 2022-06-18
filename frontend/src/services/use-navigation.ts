import type { RouteParamsRaw } from 'vue-router';
import { app } from '@/services/app-services';

export default function useNavigation() {
  const { router } = app;

  const navigateByViewName = async (name: string) => router.push({ name });

  const navigateByPath = async (path: string) => router.push(path);

  const navigateToView = async ({
    name,
    params,
  }: { name: string; params: RouteParamsRaw }) => router.push({
    name,
    params,
  });

  return {
    navigateByViewName,
    navigateToView,
    navigateByPath,
  };
}
