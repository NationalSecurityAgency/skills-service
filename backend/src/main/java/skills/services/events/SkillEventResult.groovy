package skills.services.events

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder

@Canonical
@Builder
@CompileStatic
class SkillEventResult {
    String skillId
    String name
    int pointsEarned = 0
    boolean skillApplied = true
    // only really applicable if it wasn't performed
    String explanation = "Skill event was applied"
    List<CompletionItem> completed = []
}
