import Vue from 'vue';
import moment from 'moment';

const dateFormatter = value => moment(value).format('YYYY-MM-DD HH:mm');
Vue.filter('date', dateFormatter);


// this allows to call this function from an js code; to learn more about that read about javascript modules
// import DateFilter from 'src/DateFilter.js'
//    DateFilter(dateStrVAlue)
export default dateFormatter;
