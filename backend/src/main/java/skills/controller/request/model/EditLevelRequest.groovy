package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class EditLevelRequest {
    String name
    String iconClass
    //this might be edited if you want to collapse an existing level, say you remove level 2 out of 5
    //and want to re-number the levels and edit the points/percent so that level 3 spans what used to be 2+3
    Integer level
    Integer percent
    Integer pointsFrom
    Integer pointsTo
}
