import axios from 'axios';

export default {
  subjectWithNameExists(projectId, subjectName) {
    return axios.get(`/admin/projects/${projectId}/subjectExists?subjectName=${subjectName}`)
      .then(remoteRes => !remoteRes.data);
  },
  subjectWithIdExists(projectId, subjectId) {
    return axios.get(`/admin/projects/${projectId}/subjectExists?subjectId=${subjectId}`)
      .then(remoteRes => !remoteRes.data);
  },
};
