import Vue from 'vue';

import NumberFilter from '@/common/filter/NumberFilter.js';

describe('NumberFilter', () => {
  it('exists', () => {
    expect(NumberFilter).toBeDefined();
    expect(typeof NumberFilter).toBe('function');
  });

  it('adds the global filter to Vue', () => {
    expect(Vue.options.filters.number).toBeDefined();
    expect(Vue.options.filters.number).toBe(NumberFilter);
  });

  it('properly formats numbers', () => {
    let input = 1234;
    expect(NumberFilter(input)).toEqual('1,234');

    input = 12345678;
    expect(NumberFilter(input)).toEqual('12,345,678');

    input = 123;
    expect(NumberFilter(input)).toEqual('123');

    input = 1233.63;
    expect(NumberFilter(input)).toEqual('1,234');
  });

  it('properly handles the fractionSize argument', () => {
    let input = 1234.1234556;
    expect(NumberFilter(input)).toEqual('1,234');

    input = 1234.12315126;
    expect(NumberFilter(input, 4)).toEqual('1,234.1232');
  });
});
