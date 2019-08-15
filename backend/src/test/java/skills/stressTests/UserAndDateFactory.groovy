package skills.stressTests

import groovy.time.TimeCategory

class UserAndDateFactory {

    int numUsers = 10000
    int numDates = 365

    Random r = new Random()
    String getUserId(){
        int ranNum = r.nextInt(numUsers)
        return "User${ranNum}"
    }

    Date getDate(){
        int ranNum = r.nextInt(numDates)
        use (TimeCategory) {
            return ranNum.days.ago
        }
    }
}
