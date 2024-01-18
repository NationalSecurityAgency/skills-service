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
import 'primeflex/primeflex.css'
// import 'primevue/resources/themes/lara-light-green/theme.css'
import Tooltip from 'primevue/tooltip';

const app = createApp(App)

app.use(router)
app.use(store)
app.use(PrimeVue)
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
app.directive('tooltip', Tooltip);

app.mount('#app')
