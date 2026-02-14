import type { RouteParamsRaw } from 'vue-router';
import { useRouter } from 'vue-router';

export default function useNavigation() {
  const router = useRouter();

  const navigateByViewName = async (name: string | symbol) => router.push({ name });

  const navigateByPath = async (path: string) => router.push(path);

  const navigateToView = async ({ name, params }: { name: string; params: RouteParamsRaw }) =>
    router.push({
      name,
      params,
    });

  return {
    navigateByViewName,
    navigateToView,
    navigateByPath,
  };
}
