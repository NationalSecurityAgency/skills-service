package skills.storage.repos

interface UserAchievedCustomRepo {

    static class UserAndLevel {
        String userId
        List<ProjectAndLevel> levels
    }

    static class ProjectAndLevel {
        String projectId
        Integer level

    }

    Integer countUsersWithMaxLevelForMultipleProjects(List<ProjectAndLevel> projectAndLevels)
    List<UserAndLevel> findUsersWithMaxLevelForMultipleProjects(List<ProjectAndLevel> projectAndLevels, int firstResult, int maxResults, boolean userIdSortAsc)

}
