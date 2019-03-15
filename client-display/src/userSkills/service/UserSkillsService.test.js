import UserSkillsService from '@/userSkills/service/UserSkillsService.js';

import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';

describe('userSkillsService', () => {
  let mockHttp;

  beforeEach(() => {
    mockHttp = new MockAdapter(axios);
    UserSkillsService.setServiceUrl('http://no-where');
  });

  afterEach(() => mockHttp.reset());

  it('getUserSkills calls the appropriate endpoint', (done) => {
    const mockUserSkills = { mockUserSkills: Math.random() };
    mockHttp.onGet('/api/getUserSkills', { params: { dn: 'testDn' } }).reply(200, mockUserSkills);
    UserSkillsService.getUserSkills('testDn')
      .then((result) => {
        expect(result).toEqual(mockUserSkills);
        done();
      });
  });

  it('addUserSkill calls the appropriate endpoint', (done) => {
    const mockResponse = { result: 'data' };
    mockHttp.onGet('/api/addSkill', { params: { skillId: 'testUserSkillId' } }).reply(200, mockResponse);
    UserSkillsService.addUserSkill('testUserSkillId')
      .then((result) => {
        expect(result).toEqual(mockResponse);
        done();
      });
  });
});
