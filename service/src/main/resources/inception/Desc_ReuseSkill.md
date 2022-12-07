In-Project Skill Reuse is a feature that facilitates the reuse of skills within the same project. A skill can be easily reused within another Subject or Skill Group. Reused skills are read-only copies and cannot be changed. As skill occurrences are reported to the original skill they are also automatically propagated to the reused skills. Changes to the original skill (ex. description, occurrences) are automatically synchronized to all the reused skills as well.

To reuse a skill navigate to the Skills page (`Project -> Subject`), then select skills to reuse and click on the `Action` button located on the top-right above the skills' table. Select the `Reuse in this Project` button to initiate the process.

Once the skills are reused you will see a `REUSED` tag next to each skill.

> **TIP**
> All the skills under a subject or group can be easily selected for reuse via the `Select All` button

### Skill occurrences propagation

Please note that when skill occurrences are reported for an original skill, SkillTree checks whether that skill was reused. For every reused instance the occurrence is queued up to be propagated to that reused skills. The queue is handled in an asynchronous manner so there is a small delay before the skill occurrence are reflected in the reused skills.

### Modify skill in the original project

Only the original skill can be updated. After the attributes of the original skill are changed they are queued up to be propagated to all reused versions of the edited skill. Please note that skill attribute propagation is performed asynchronously, it may take a few moments for changes to appear in all reused skills.

### Self Reporting and Reused Skills

As skill occurrences are reported to the original skill they are also automatically propagated to all reused copies. Generally skill occurrences are not allowed to be reported against a reused skill copy, after all it is meant to be read-only.

The one exception to this restriction is self-reported skills. Self-reported skills can be reported to the original skill OR to any of its reused copies. Regardless of whether it was reported against a reused copy or the natively declared skill, the points are applied to the original AND all of its copies (assuming points are due based on the current configuration and the reported user's prior contributions). When a self-reported skill occurrence is reported to the reused copy then the request is simply routed to the originally-declared skill. From there on the flow is identical to any skill occurrence that is being reported to the original skill and for every reused instance the occurrence is queued up to be propagated to that reused skills. The queue is handled in an asynchronous manner so there is a small delay before the skill occurrence are reflected in the reused skills.

The Self-reporting `I did it` button is displayed for each reused skill in the Skills Display and Progress and Rankings views.

### Other Considerations

* Skills with dependencies can NOT be reused. Reused skills can NOT be added as a dependency.
* Reused skills cannot be added to a badge; use the original skill instead
* Removing the original skill will also delete all of its reused copies