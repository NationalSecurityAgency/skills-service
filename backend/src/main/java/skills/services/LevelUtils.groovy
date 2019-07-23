package skills.services

import groovy.util.logging.Slf4j
import skills.storage.model.LevelDef

@Slf4j
class LevelUtils {

    static final double SCALE_THRESHOLD = 1.3

    static int defaultTotalPointsGuess = 1000

    public void convertToPoints(List<LevelDef> levelDefs, int totalPoints){
        List<Integer> levelScores = levelDefs.sort({ it.level }).collect {
            return (int) (totalPoints * (it.percent / 100d))
        }
        levelDefs.eachWithIndex{ LevelDef entry, int i ->
            Integer fromPts = levelScores.get(i)
            Integer toPts = (i != levelScores.size() - 1) ? levelScores.get(i + 1) : null
            entry.pointsFrom = fromPts
            entry.pointsTo = toPts
        }
    }

    public void convertToPercentage(List<LevelDef> levelDefs, int totalPoints){
        levelDefs.sort({ it.level })

        Integer highestLevelPoints = levelDefs?.last()?.pointsFrom ? levelDefs.last().pointsFrom : 0

        double scaler = 1.0;

        if(highestLevelPoints > totalPoints){
            //this means that skills were deleted since the levels were converted to points/edited
            //if we convert as-is to percentages, we'll wind up with invalid percentage values
            log.warn("totalPoints [$totalPoints] are lower " +
                    "then the highest level's pointsFrom [${highestLevelPoints}], " +
                    "this would create invalid percentage values. Using [${highestLevelPoints}] as totalPoints for purposes of conversion")
            //since we don't know what the total was before deletion, let's model the percentages off the highest level points being 92%
            //since that's what we do for the default, otherwise the last level would be 100%
            totalPoints = highestLevelPoints*1.08
        } else if (SCALE_THRESHOLD*highestLevelPoints < totalPoints){
            //this will result in an approximation as we don't know for sure the user's original intent
            //but it will at least make more sense then leaving it untouched in this scenario.
            log.info("skills were added after defining levels as points, attempting to scale the current point posture to the total points")
            scaler = totalPoints/highestLevelPoints.toDouble()
        }

        LevelDef lastEntry = null

        int totalLevels = levelDefs.size()

        levelDefs.eachWithIndex { LevelDef entry, int i ->
            if (entry.pointsFrom != null) {
                double scaled = entry.pointsFrom * scaler
                entry.percent = (int) ((scaled / totalPoints) * 100d)
                if (entry.percent == 0) {
                    //this can happen if someone adds a skill with a very large range of points after
                    //a conversion to points happens. The first few level points could be such a small percentage
                    //of the total that they would be effectively zero. Lets prevent that and use some sort of sensible default
                    //in those cases
                    entry.percent += 10 + (lastEntry?.percent ? lastEntry.percent : 0)
                }
            } else {
                //this could happen if there is an empty subject with no skills
                entry.percent = (((100/totalLevels)*(i+1)))-8
            }
            lastEntry = entry
        }
    }
}
