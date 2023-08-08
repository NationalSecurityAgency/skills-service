/*
 * Copyright 2020 SkillTree
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
import VueApexCharts from 'vue-apexcharts';

import Vue from 'vue';
import VueAnnouncer from '@vue-a11y/announcer';
import {
    ModalPlugin,
    DropdownPlugin,
    FormInputPlugin,
    ButtonPlugin,
    TablePlugin,
    PaginationPlugin,
    BadgePlugin,
    ProgressPlugin,
    SpinnerPlugin,
    TooltipPlugin,
    FormTextareaPlugin,
    CardPlugin,
    OverlayPlugin,
    CollapsePlugin,
    LinkPlugin,
    FormCheckboxPlugin,
} from 'bootstrap-vue';
import {
  localize, ValidationProvider, ValidationObserver, setInteractionMode,
} from 'vee-validate';
import en from 'vee-validate/dist/locale/en.json';
import FiltersPlugin from '@/common-components/filter/FiltersPlugin';
import TimeDurationFilter from '@/common-components/filter/TimeDurationFilter';
import '@/common-components/filter/ByteFilter';
import App from '@/App';
import router from '@/router';
import store from '@/store/store';
import 'apexcharts';
import '@/common/filter/DayJsFilters';
import DevModeUtil from '@/dev/DevModeUtil';

Vue.config.productionTip = false;

Vue.component('ValidationProvider', ValidationProvider);
Vue.component('ValidationObserver', ValidationObserver);

Vue.use(VueApexCharts);
Vue.use(ModalPlugin);
Vue.use(DropdownPlugin);
Vue.use(FormInputPlugin);
Vue.use(ButtonPlugin);
Vue.use(TablePlugin);
Vue.use(PaginationPlugin);
Vue.use(BadgePlugin);
Vue.use(ProgressPlugin);
Vue.use(SpinnerPlugin);
Vue.use(TooltipPlugin);
Vue.use(FormTextareaPlugin);
Vue.use(CardPlugin);
Vue.use(OverlayPlugin);
Vue.use(FiltersPlugin);
Vue.use(CollapsePlugin);
Vue.use(LinkPlugin);
Vue.use(FormCheckboxPlugin);
Vue.use(TimeDurationFilter);
Vue.use(VueAnnouncer);

localize({
  en,
});
setInteractionMode('custom', () => ({ on: ['input', 'change'] }));

require('@/common/interceptors/softwareVersionInterceptor');
require('@/common/interceptors/upgradeInProgressInterceptor');

const initializeVueApp = () => {
  new Vue({
    router,
    store,
    render: (h) => h(App),
  }).$mount('#app');
};

if (DevModeUtil.isDevelopmentMode()) {
  DevModeUtil.configureDevelopmentMode(store).then(() => { initializeVueApp(); });
} else {
  initializeVueApp();
}
