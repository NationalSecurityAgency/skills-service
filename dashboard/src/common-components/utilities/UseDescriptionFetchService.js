import axios from "axios";

export const useDescriptionFetchService = () => {

    const getDescriptionForSkill = (projectId, skillId) => {
        let url = `/api/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/description`
        return axios.get(url).then((result) => result.data)
    }

    return {
        getDescriptionForSkill
    }
}