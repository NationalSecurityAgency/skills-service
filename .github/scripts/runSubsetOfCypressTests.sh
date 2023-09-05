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
allTests=($(find . | grep -i -E 'spec[s]?\.js' | sort))
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

echo "Cypres tests:"
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
  printf "\nRunning $count: $testClass ----------------------------------\n"
  count=$((count+1))

  baseDir=$(echo "${testClass}" | cut -d "/" -f2)
  customSnapDir=""
  if [ "${baseDir}" != "" ];then
    if [[ ${baseDir} != *"js" ]];then
     customSnapDir="--env customSnapshotsDir=./cypress/snapshots/${baseDir} "
    fi
  fi

#  commandToRun="TZ=UTC cypress run --browser chrome${customSnapDir} --config baseUrl=${baseSkillTreeUrl} --spec \"${testClass}\""
  commandToRun="npm run cy:run:dev -- ${customSnapDir}--spec cypress/e2e/${testClass}"
  echo "Running command [${commandToRun}]"
  exec $commandToRun
done



#IFS=,;
#testsExecString="${testsInThisRun[*]}"
#verifyExecString=""
#if (( ${#integrationTests[@]} != 0 )); then
#  integrationTestsString="${integrationTests[*]}"
#  integrationTestsExecString="-Dit.test="$integrationTestsString""
#  verifyExecString="verify"
#fi
#unset IFS

#echo ${testsExecString}
#commandToRun="mvn ${additionalTestVars} -Dtest="${serviceTestsExecString}" ${integrationTestsExecString} test ${verifyExecString}"
#commandToRun="TZ=UTC cypress run --browser chrome --config baseUrl=http://localhost:8082 --spec \"cypress/e2e/discoverable_proj_invite_spec.js\""
#echo $commandToRun
#exec $commandToRun
