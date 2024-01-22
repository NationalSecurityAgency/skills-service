import './assets/main.css'

import { createApp } from 'vue'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import App from './App.vue'
import router from './router'
import store from '@/store/store'

import Button from 'primevue/button'
import Card from 'primevue/card'
import Panel from 'primevue/panel'
import Toast from 'primevue/toast'
import Avatar from 'primevue/avatar'
import InputText from 'primevue/inputtext'
import Divider from 'primevue/divider'
import Message from 'primevue/message'
import Menu from 'primevue/menu'
import ProgressSpinner from 'primevue/progressspinner'
import Breadcrumb from 'primevue/breadcrumb'
import Dropdown from 'primevue/dropdown';
import Dialog from 'primevue/dialog';
import InputSwitch from 'primevue/inputswitch';
import Tooltip from 'primevue/tooltip';
import Ripple from 'primevue/ripple';
import FocusTrap from 'primevue/focustrap';
import 'primeflex/primeflex.css'
import '@fortawesome/fontawesome-free/css/all.css'
// import 'primevue/resources/themes/lara-light-green/theme.css'

const app = createApp(App)

app.use(router)
app.use(store)
app.use(PrimeVue, { ripple: true })
app.use(ToastService)
app.component('Button', Button)
app.component('Card', Card)
app.component('Panel', Panel)
app.component('Toast', Toast)
app.component('Avatar', Avatar)
app.component('InputText', InputText)
app.component('Divider', Divider)
app.component('Message', Message)
app.component('Menu', Menu)
app.component('ProgressSpinner', ProgressSpinner)
app.component('Breadcrumb', Breadcrumb)
app.component('Dropdown', Dropdown)
app.component('Dialog', Dialog)
app.component('InputSwitch', InputSwitch)
app.directive('tooltip', Tooltip);
app.directive('ripple', Ripple)
app.directive('focustrap', FocusTrap);

app.mount('#app')
