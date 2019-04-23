import Vue from 'vue';

const wordFormatter = (value, number) => {
  let formatString = value;
  if (number > 1) {
    formatString += 's';
  }
  return formatString;
};

Vue.filter('plural', wordFormatter);

export default wordFormatter;
