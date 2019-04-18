package skills.service.controller.result.model

class LevelDefinitionRes {
    String id
    String projectId

    String skillId

    int level
    Integer percent

    Integer pointsFrom
    Integer pointsTo

    String iconClass
    String name

    boolean achievable = true
}
