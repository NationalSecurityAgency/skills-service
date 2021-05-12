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
import App from '@/App';
import marked from 'marked';
import router from '@/router';
import store from '@/store';
import 'apexcharts';
import '@/common/filter/DayJsFilters';

import {
    ModalPlugin,
    DropdownPlugin,
    FormInputPlugin,
    ButtonPlugin,
    TablePlugin,
    BadgePlugin,
    ProgressPlugin,
    SpinnerPlugin,
} from 'bootstrap-vue';

Vue.config.productionTip = false;

Vue.use(VueApexCharts);
Vue.use(ModalPlugin);
Vue.use(DropdownPlugin);
Vue.use(FormInputPlugin);
Vue.use(ButtonPlugin);
Vue.use(TablePlugin);
Vue.use(BadgePlugin);
Vue.use(ProgressPlugin);
Vue.use(SpinnerPlugin);

require('@/common/softwareVersion/softwareVersionInterceptor');

const renderer = new marked.Renderer();
renderer.link = function markedLinkRenderer(href, title, text) {
  let titleRes = title;
  if (!title) {
    titleRes = text;
  }
  const link = marked.Renderer.prototype.link.call(this, href, titleRes, text);
  let resLink = link.replace('<a', "<a target='_blank' ");
  resLink = resLink.replace('</a>', ' <i class="fas fa-external-link-alt" style="font-size: 0.8rem"></i></a>');
  return resLink;
};
marked.setOptions({
  renderer,
});

new Vue({
    router,
    store,
    render: (h) => h(App),
}).$mount('#app');
