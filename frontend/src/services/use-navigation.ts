import type { LocationQueryRaw, RouteParamsRaw } from 'vue-router';
import { useRouter } from 'vue-router';

export default function useNavigation() {
  const router = useRouter();

  const navigateByViewName = async (name: string | symbol) => router.push({ name });

  const navigateByPath = async (path: string) => router.push(path);

  const navigateToView = async ({
    name,
    params,
    query,
  }: { name: string; params?: RouteParamsRaw; query?: LocationQueryRaw }) => router.push({
    name,
    params,
    query,
  });

  return {
    navigateByViewName,
    navigateToView,
    navigateByPath,
  };
}
