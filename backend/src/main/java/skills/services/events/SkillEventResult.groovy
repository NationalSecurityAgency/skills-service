package skills.services.events

import groovy.transform.Canonical
import groovy.transform.builder.Builder

@Canonical
@Builder
class SkillEventResult {
    boolean success = true
    boolean skillApplied = true
    // only really applicable if it wasn't performed
    String explanation = "Skill event was applied"
    List<CompletionItem> completed = []
}
