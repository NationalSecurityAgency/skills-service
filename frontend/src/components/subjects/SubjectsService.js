import axios from 'axios';

export default {
  subjectWithNameExists(projectId, subjectName) {
    return axios.get(`/admin/projects/${projectId}/subjectExists?subjectName=${encodeURIComponent(subjectName)}`)
      .then(remoteRes => !remoteRes.data);
  },
  subjectWithIdExists(projectId, subjectId) {
    return axios.get(`/admin/projects/${projectId}/subjectExists?subjectId=${subjectId}`)
      .then(remoteRes => !remoteRes.data);
  },
  getSubjectDetails(projectId, subjectId) {
    return axios.get(`/admin/projects/${projectId}/subjects/${subjectId}`)
      .then(res => res.data);
  },
  getSubjects(projectId) {
    return axios.get(`/admin/projects/${projectId}/subjects`)
      .then(res => res.data);
  },
  saveSubject(subject) {
    return axios.post(`/admin/projects/${subject.projectId}/subjects/${subject.subjectId}`, subject)
      .then(() => this.getSubjectDetails(subject.projectId, subject.subjectId));
  },
  patchSubject(subject, actionToSubmit) {
    return axios.patch(`/admin/projects/${subject.projectId}/subjects/${subject.subjectId}`, { action: actionToSubmit });
  },
  deleteSubject(subject) {
    return axios.delete(`/admin/projects/${subject.projectId}/subjects/${subject.subjectId}`);
  },
};
