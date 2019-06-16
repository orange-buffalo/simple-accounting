import api from '@/services/api'

export const userApi = {

  getCurrencies: function () {
    return new Promise((resolve, reject) => {
      api.get('/currencies')
          .then(response => {
            resolve(response.data)
          })
          .catch(error => reject(error))
    })
  }

}

export default userApi