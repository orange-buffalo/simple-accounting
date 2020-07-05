import { app } from '@/services/app-services';

export default function useNavigation() {
  const navigateByViewName = async (name) => app.router.push({ name });

  const navigateToView = async ({ name, params }) => app.router.push({
    name,
    params,
  });

  return {
    navigateByViewName,
    navigateToView,
  };
}
