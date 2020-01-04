import { api } from '@/services/api';

export const userApi = {

  // todo #14: use on profile page
  async getProfile() {
    const { data: profile } = await api.get('/profile');
    return profile;
  },

};

export default userApi;
