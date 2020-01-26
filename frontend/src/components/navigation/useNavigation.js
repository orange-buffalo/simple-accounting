import { app } from '@/services/app-services';

export default function useNavigation() {
  const navigateByViewName = async name => app.router.push({ name });

  return {
    navigateByViewName,
  };
}
