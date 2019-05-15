import axios from 'axios';
import router from './router';
import store from './store/store';

function errorResponseHandler(error) {
  // check if the caller wants to handle the error with displaying the errorPage/dialog
  if (Object.prototype.hasOwnProperty.call(error.config, 'handleError') && error.config.handleError === false) {
    return Promise.reject(error);
  }

  const errorCode = error.response ? error.response.status : undefined;
  if (errorCode === 401) {
    store.commit('clearAuthData');
    const path = window.location.pathname;
    if (path !== '/skills-login') {
      const loginRoute = path !== '/' ? { name: 'Login', query: { redirect: path } } : { name: 'Login' };
      router.push(loginRoute);
    }
  } else {
    router.push({ name: 'ErrorPage' });
  }
  return Promise.reject(error);
}

// apply interceptor on response
axios.interceptors.response.use(
  response => response,
  errorResponseHandler,
);

export default errorResponseHandler;
