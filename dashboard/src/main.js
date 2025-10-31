/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import App from './App.vue'
import constructRouter from './router'
import VueAnnouncer from '@vue-a11y/announcer'
import VueApexCharts from 'vue3-apexcharts'
import log from 'loglevel'

import {Select, ToggleSwitch} from "primevue";
import Button from 'primevue/button'
import ButtonGroup from 'primevue/buttongroup'
import Card from 'primevue/card'
import Panel from 'primevue/panel'
import Toast from 'primevue/toast'
import Avatar from 'primevue/avatar'
import InputText from 'primevue/inputtext'
import Divider from 'primevue/divider'
import Menu from 'primevue/menu'
import ProgressSpinner from 'primevue/progressspinner'
import Breadcrumb from 'primevue/breadcrumb'
import Dialog from 'primevue/dialog'
import BlockUI from 'primevue/blockui'
import SelectButton from 'primevue/selectbutton'
import Badge from 'primevue/badge'
import MultiSelect from 'primevue/multiselect'
import InputNumber from 'primevue/inputnumber'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'
import Fieldset from 'primevue/fieldset'
import ToggleButton from 'primevue/togglebutton'
import RadioButton from 'primevue/radiobutton'
import Checkbox from 'primevue/checkbox'
import Rating from 'primevue/rating'
import Textarea from 'primevue/textarea'
import Listbox from 'primevue/listbox'
import ProgressBar from 'primevue/progressbar'
import Chip from 'primevue/chip'
import FloatLabel from "primevue/floatlabel";
import Timeline from 'primevue/timeline';
import { Sortable, Swap } from 'sortablejs';

Sortable.mount(new Swap())

import ConfirmationService from 'primevue/confirmationservice'
import BadgeDirective from 'primevue/badgedirective'

import FocusTrap from 'primevue/focustrap'

import Message from '@/components/utils/misc/Message.vue'
import InlineMessage from '@/components/utils/misc/InlineMessage.vue'
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue'
import SkillsTextInput from '@/components/utils/inputForm/SkillsTextInput.vue'
import SkillsIdInput from '@/components/utils/inputForm/SkillsIdInput.vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import SkillsNumberInput from '@/components/utils/inputForm/SkillsNumberInput.vue'
import SkillsCheckboxInput from '@/components/utils/inputForm/SkillsCheckboxInput.vue'
import SkillsRadioButtonInput from '@/components/utils/inputForm/SkillsRadioButtonInput.vue'
import SkillsTextarea from '@/components/utils/inputForm/SkillsTextarea.vue'
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import SkillsCardHeader from '@/components/utils/cards/SkillsCardHeader.vue'
import { useSkillsReporterDirective } from '@/components/utils/SkillsReporterDirective.js'

import '@fortawesome/fontawesome-free/css/all.css'
import 'material-icons/css/material-icons.css'
import 'material-icons/iconfont/material-icons.css'
import '@toast-ui/editor/dist/toastui-editor.css'
import 'video.js/dist/video-js.css'
import defineSkillTreePreset from "@/theme/StPrimeVueThemePreset.js";


log.setLevel('info')

const pinia = createPinia()

const app = createApp(App)
const router = constructRouter()

app.use(router)
app.use(pinia)
app.use(PrimeVue, {
    theme: {
        preset: defineSkillTreePreset(),
        options: {
            prefix: 'p',
            darkModeSelector: '.st-dark-theme',
            cssLayer: false
        }
    }
})
app.use(ToastService)
app.use(VueAnnouncer, { router })
app.use(VueApexCharts)
app.use(ConfirmationService)
app.component('Button', Button)
app.component('ButtonGroup', ButtonGroup)
app.component('Card', Card)
app.component('Panel', Panel)
app.component('Toast', Toast)
app.component('Avatar', Avatar)
app.component('InputText', InputText)
app.component('Divider', Divider)
app.component('Message', Message)
app.component('InlineMessage', InlineMessage)
app.component('Menu', Menu)
app.component('ProgressSpinner', ProgressSpinner)
app.component('Breadcrumb', Breadcrumb)
app.component('Select', Select)
app.component('Dialog', Dialog)
app.component('ToggleSwitch', ToggleSwitch)
app.component('BlockUI', BlockUI)
app.component('SelectButton', SelectButton)
app.component('Badge', Badge)
app.component('SkillsSpinner', SkillsSpinner)
app.component('MultiSelect', MultiSelect)
app.component('Tag', Tag)
app.component('DataTable', DataTable)
app.component('Column', Column)
app.component('InputNumber', InputNumber)
app.component('InputGroup', InputGroup)
app.component('InputGroupAddon', InputGroupAddon)
app.component('Fieldset', Fieldset)
app.component('ToggleButton', ToggleButton)
app.component('RadioButton', RadioButton)
app.component('Checkbox', Checkbox)
app.component('Rating', Rating)
app.component('Textarea', Textarea)
app.component('Listbox', Listbox)
app.component('ProgressBar', ProgressBar)
app.component('Chip', Chip)
app.component('FloatLabel', FloatLabel)
app.component('Timeline', Timeline)

app.component('SkillsButton', SkillsButton)
app.component('SkillsTextInput', SkillsTextInput)
app.component('SkillsIdInput', SkillsIdInput)
app.component('SkillsDialog', SkillsDialog)
app.component('SkillsNumberInput', SkillsNumberInput)
app.component('SkillsCheckboxInput', SkillsCheckboxInput)
app.component('SkillsRadioButtonInput', SkillsRadioButtonInput)
app.component('SkillsTextarea', SkillsTextarea)
app.component('SkillsDropDown', SkillsDropDown)
app.component('SkillsDataTable', SkillsDataTable)
app.component('SkillsCardHeader', SkillsCardHeader)

const skillsReporterDirective = useSkillsReporterDirective();
app.directive('skills', skillsReporterDirective.vSkills);
app.directive('skills-onMount', skillsReporterDirective.vSkillsOnMounted);
app.directive('focustrap', FocusTrap);
app.directive('badge', BadgeDirective);

app.mount('#app')
