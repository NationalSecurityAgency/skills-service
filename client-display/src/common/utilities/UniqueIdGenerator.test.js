import UniqueIdGenerator from '@/common/utilities/UniqueIdGenerator.js';

describe('UniqueIdGenerator', () => {
  describe('uniqueId', () => {
    it('takes a optional prefix', () => {
      const mockPrefix = Math.random();

      expect(UniqueIdGenerator.uniqueId(mockPrefix)).toBe(`${mockPrefix}1`);
    });

    it('prefix is optional', () => {
      expect(UniqueIdGenerator.uniqueId()).toBe('2');
    });
  });
});
