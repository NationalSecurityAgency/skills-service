export default {
  toInt(value) {
    let numValue = value;
    if (typeof (value) === 'string' || value instanceof String) {
      numValue = parseInt(value, 10);
    }
    return numValue;
  },
};
