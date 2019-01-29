import Vue from 'vue';
import numeral from 'numeral';

const numberFormatter = value => numeral(value).format('0,0');
Vue.filter('number', numberFormatter);


// this allows to call this function from an js code; to learn more about that read about javascript modules
// import NumberFilter from 'src/NumberFilter.js'
//    NumberFilter(myNumber)
export default numberFormatter;
