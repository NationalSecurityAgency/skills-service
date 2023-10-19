#!/bin/bash
usage() { echo "Usage runSubsetOfCypressTests.sh -c <current_run> -t <total_concurrent_runs>" 1>&2; exit 1; }

additionalTestVars=""
while getopts ":c:t:d:" flag;
do
    case "${flag}" in
        c)
          currentRun=${OPTARG}
          ;;
        t)
          totalConcurrent=${OPTARG}
          ;;
        d)
          additionalTestVars=${OPTARG}
          ;;
        *)
          usage
          ;;
    esac
done
if [ -z "${currentRun}" ] || [ -z "${totalConcurrent}" ]; then
    usage
fi

baseSkillTreeUrl="http://localhost:8082"
echo "Running subset of service tests"
echo "Current Run: $currentRun";
echo "Additional Vars: $additionalTestVars";
echo "Total Concurrent: $totalConcurrent";
echo "Base SkillTree URL: $baseSkillTreeUrl"

cd ./cypress/e2e
IFS=$'\n'
# locate all of the tests files then "shuffle" in a repetitive order by sorting using md5sum
allTests=($(find . -type f -exec md5sum {} + | grep -i -E 'spec[s]?\.js' | sort | cut -d " " -f 3))
#allTests=($(find . | grep -i -E 'spec[s]?\.js' | sort | cut -d " " -f 3))
unset IFS
cd ../../

totalNum=${#allTests[@]}
numPerRun=$((totalNum/totalConcurrent))
start=$(((currentRun-1)*numPerRun))
if  [ "$currentRun" -eq "$totalConcurrent" ]; then
    numPerRun=$totalNum
fi
testsInThisRun=${allTests[@]:$start:$numPerRun}
echo "There are [$totalNum] total tests. Run [$currentRun]: Start Number=[$start], running up to [$numPerRun] tests"

echo "Cypress tests:"
returnCode=0
GREEN='\033[0;32m'
RED='\033[0;31m'
NOCOLOR='\033[0m'
testResultOutput="Test Results:\n"
count=1;
for testClass in ${testsInThisRun[@]}
do
  echo "$count: $testClass"
  count=$((count+1))
done
echo "------------------------------------------------"
count=1;
for testClass in ${testsInThisRun[@]}
do
  testNum=$count
  printf "\nRunning $testNum: $testClass ----------------------------------\n"
  count=$((count+1))

  commandToRun="npm run cy:run -- --spec cypress/e2e/${testClass}"
  echo "Running command [${commandToRun}]"
  eval $commandToRun
  commandStatusCode=$?
  if [ $commandStatusCode -ne 0 ];
  then
    returnCode=$commandStatusCode
    testResultOutput="${testResultOutput}Test $testNum: ${RED}\u274c FAILED${NOCOLOR} => ${testClass}\n"
  else
    testResultOutput="${testResultOutput}Test $testNum: ${GREEN}\u2714 PASSED${NOCOLOR} => ${testClass}\n"
  fi
done

echo "-------------------"
echo -e $testResultOutput
echo "-------------------"
echo "Thanks for testing! Return code is [${returnCode}]"
exit $returnCode
