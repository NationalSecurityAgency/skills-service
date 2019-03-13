import Vue from 'vue';
import numeral from 'numeral';

const numberFormatter = (value, fractionSize) => {
  let formatString = '0,0';
  if (fractionSize || fractionSize === 0) {
    formatString = `${formatString}[.]${'0'.repeat(fractionSize)}`;
  }
  return numeral(value).format(formatString);
};

Vue.filter('number', numberFormatter);

export default numberFormatter;
