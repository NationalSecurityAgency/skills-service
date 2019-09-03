package skills.services.events

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@Canonical
@CompileStatic
class CompletionItem {
    static enum CompletionItemType {
        Overall, Subject, Skill, Badge, GlobalBadge
    };
    CompletionItemType type
    Integer level // optional
    String id
    String name
}
