import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import App from './App.vue'
import router from './router'
import store from '@/store/store'
import VueAnnouncer from '@vue-a11y/announcer'

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
import BlockUI from 'primevue/blockui';
import SelectButton from 'primevue/selectbutton';
import Badge from 'primevue/badge';
import MultiSelect from 'primevue/multiselect'
import InputNumber from 'primevue/inputnumber'
import Tag from 'primevue/tag';
import DataTable from 'primevue/datatable';
import InputGroup from 'primevue/inputgroup';
import InputGroupAddon from 'primevue/inputgroupaddon';
import Fieldset from 'primevue/fieldset';

import BadgeDirective from 'primevue/badgedirective';


import Tooltip from 'primevue/tooltip';
import FocusTrap from 'primevue/focustrap';

import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue'
import SkillsTextInput from '@/components/utils/inputForm/SkillsTextInput.vue'
import SkillsIdInput from '@/components/utils/inputForm/SkillsIdInput.vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import SkillsNumberInput from '@/components/utils/inputForm/SkillsNumberInput.vue'

import 'primeflex/primeflex.css'
import '@fortawesome/fontawesome-free/css/all.css'
import '@toast-ui/editor/dist/toastui-editor.css';
// import 'primevue/resources/themes/lara-light-green/theme.css'
const pinia = createPinia()

const app = createApp(App)

app.use(router)
app.use(store)
app.use(pinia)
app.use(PrimeVue)
app.use(ToastService)
app.use(VueAnnouncer)
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
app.component('BlockUI', BlockUI)
app.component('SelectButton', SelectButton)
app.component('Badge', Badge)
app.component('SkillsSpinner', SkillsSpinner)
app.component('MultiSelect', MultiSelect)
app.component('Tag', Tag)
app.component('DataTable', DataTable)
app.component('InputNumber', InputNumber)
app.component('InputGroup', InputGroup)
app.component('InputGroupAddon', InputGroupAddon)
app.component('Fieldset', Fieldset)

app.component('SkillsButton', SkillsButton)
app.component('SkillsTextInput', SkillsTextInput)
app.component('SkillsIdInput', SkillsIdInput)
app.component('SkillsDialog', SkillsDialog)
app.component('SkillsNumberInput', SkillsNumberInput)

app.directive('tooltip', Tooltip);
app.directive('focustrap', FocusTrap);
app.directive('badge', BadgeDirective);

app.mount('#app')
