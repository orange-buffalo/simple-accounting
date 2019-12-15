import { api } from '@/services/api';

export const userApi = {

  getCurrencies() {
    return new Promise((resolve, reject) => {
      api.get('/currencies')
        .then((response) => {
          resolve(response.data);
        })
        .catch(error => reject(error));
    });
  },

  // todo #6 use on profile page
  async getProfile() {
    const { data: profile } = await api.get('/profile');
    return profile;
  },

};

export default userApi;
