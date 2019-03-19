import axios from 'axios';
import { ModalProgrammatic } from 'buefy/dist/components/modal';
import router from './router';
import store from './store/store';
import ErrorModal from './components/utils/ErrorModal';

function errorResponseHandler(error) {
  // check if the caller wants to handle the error with displaying the errorPage/dialog
  if (Object.prototype.hasOwnProperty.call(error.config, 'handleError') && error.config.handleError === false) {
    return Promise.reject(error);
  }

  const errorCode = error.response ? error.response.status : undefined;
  if (errorCode === 401) {
    const urlParams = new URLSearchParams(window.location.search);
    const redirectParam = urlParams.get('redirect');
    if (!redirectParam) {
      const path = window.location.pathname;
      const loginRoute = path && path !== '/' ? { name: 'HomePage', query: { redirect: path } } : { name: 'HomePage' };
      store.commit('clearAuthData');
      router.push(loginRoute);
    }
  } else {
    const errorMessage = (error.response && error.response.data && error.response.data.message) ? error.response.data.message : undefined;
    const showModalDialog = Object.prototype.hasOwnProperty.call(error.config, 'useErrorPage') && error.config.useErrorPage === false;
    if (showModalDialog) {
      ModalProgrammatic.open({
        parent: this,
        component: ErrorModal,
        hasModalCard: true,
        props: {
          errorMessage,
        },
      });
    } else {
      router.push({ name: 'ErrorPage', query: { errorMessage } });
    }
  }
  return Promise.reject(error);
}

// apply interceptor on response
axios.interceptors.response.use(
  response => response,
  errorResponseHandler,
);

export default errorResponseHandler;
