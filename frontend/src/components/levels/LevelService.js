import axios from 'axios';

export default {
  getLevelsForProject(projectId) {
    const url = `/admin/projects/${projectId}/levels`;
    return axios.get(url).then(response => response.data);
  },
  getLevelsForSubject(projectId, subjectId) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/levels`;
    return axios.get(url).then(response => response.data);
  },
  deleteLastLevelForProject(projectId) {
    const url = `/admin/projects/${projectId}/levels/last`;
    return axios.delete(url);
  },
  deleteLastLevelForSubject(projectId, subjectId) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/levels`;
    return axios.delete(url);
  },
  createNewLevelForProject(projectId, nextLevelObject) {
    const url = `/admin/projects/${projectId}/levels/next`;
    return axios.put(url, nextLevelObject);
  },
  createNewLevelForSubject(projectId, subjectId, nextLevelObject) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/levels/next`;
    return axios.put(url, nextLevelObject);
  },
  editlevelForProject(projectId, editedLevel) {
    const url = `/admin/projects/${projectId}/levels/edit/${editedLevel.id}`;
    return axios.put(url, editedLevel);
  },
  editlevelForSubject(projectId, subjectId, editedLevel) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/levels/edit/${editedLevel.id}`;
    return axios.put(url, editedLevel);
  },
};
