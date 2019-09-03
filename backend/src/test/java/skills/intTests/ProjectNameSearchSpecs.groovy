package skills.intTests

import skills.intTests.utils.DefaultIntSpec

class ProjectNameSearchSpecs extends DefaultIntSpec {

    def setup(){
        skillsService.createProject([projectId: "proj0", name: "Name Search Proj 0"])
        skillsService.createProject([projectId: "proj3", name: "Name Search Proj 3 other"])
        skillsService.createProject([projectId: "proj2", name: "Name Search Proj 2 good"])
        skillsService.createProject([projectId: "proj1", name: "Name Search Proj 1"])
    }

    def "project search must not return itself"(){
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "Na")
        then:
        res.size() == 3
        res.collect {it.projectId} == ["proj1", "proj2", "proj3"]
    }

    def "project search must be case insensitive"(){
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "NaMe")
        then:
        res.size() == 3
        res.collect {it.projectId} == ["proj1", "proj2", "proj3"]
    }

    def "project search must search anywhere in the name"(){
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "Search Proj")
        then:
        res.size() == 3
        res.collect {it.projectId} == ["proj1", "proj2", "proj3"]
    }

    def "project search must work on the full name"(){
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "Name Search Proj 2 good")
        then:
        res.size() == 1
        res.collect {it.projectId} == ["proj2"]
    }

    def "return only first 5 projects in the result"(){
        skillsService.createProject([projectId: "proj4", name: "Name Search Proj 4"])
        skillsService.createProject([projectId: "proj5", name: "Name Search Proj 5"])
        skillsService.createProject([projectId: "proj6", name: "Name Search Proj 6"])
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "NaMe")

        then:
        res.size() == 5
        res.collect {it.projectId} == ["proj1", "proj2", "proj3", "proj4", "proj5"]
    }

    def "empty search should return first 5 projects"(){
        skillsService.createProject([projectId: "proj4", name: "Name Search Proj 4"])
        skillsService.createProject([projectId: "proj5", name: "Name Search Proj 5"])
        skillsService.createProject([projectId: "proj6", name: "Name Search Proj 6"])
        when:
        def res = skillsService.searchOtherProjectsByName("proj0", "")

        then:
        res.size() == 5
        res.collect {it.projectId} == ["proj1", "proj2", "proj3", "proj4", "proj5"]
    }
}
