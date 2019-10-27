import api from '@/services/api';

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

};

export default userApi;
