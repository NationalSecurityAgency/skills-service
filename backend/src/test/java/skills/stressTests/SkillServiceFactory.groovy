package skills.stressTests

import skills.intTests.utils.SkillsService

class SkillServiceFactory {

        static String serviceUrl = "http://localhost:8080"
//    static String serviceUrl = "http://10.113.80.23:8080"
//    static String serviceUrl = "http://internal-skills-lb-117005938.us-east-1.elb.amazonaws.com"

    static Map<String, SkillsService> cache = Collections.synchronizedMap([:])

    static synchronized SkillsService getService(String projectId) {
        assert serviceUrl
        SkillsService service = cache.get(projectId)
        if (!service) {
            service = new SkillsService("${projectId}user".toString(), "password", serviceUrl)
            cache.put(projectId, service)
        }
        return  service
    }
}
