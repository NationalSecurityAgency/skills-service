export const useByteFormat = () => {
  const prettyBytes = (bytes) => {
    if (typeof bytes !== 'number' || Number.isNaN(bytes)) {
      throw new TypeError('Expected a number');
    }
    const term = 1024;
    const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    const neg = bytes < 0;
    let retVal = bytes;

    if (neg) {
      retVal = -retVal;
    }

    if (retVal < 1) {
      return `${(neg ? '-' : '') + retVal} B`;
    }

    const exponent = Math.min(Math.floor(Math.log(retVal) / Math.log(term)), units.length - 1);
    retVal = (retVal / (term ** exponent)).toFixed(2) * 1;
    const unit = units[exponent];

    return `${(neg ? '-' : '') + retVal} ${unit}`;
  };

  return {
    prettyBytes
  }
}