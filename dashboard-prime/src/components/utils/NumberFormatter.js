import format from 'number-format.js';

export default class NumberFormatter {
    static format(value, fractionSize) {
        let formatString = '#,##0.';
        if (fractionSize || fractionSize === 0) {
            formatString = `${formatString}${'0'.repeat(fractionSize)}`;
        }
        const fv = parseFloat(value);
        return format(formatString, fv);
    };
}