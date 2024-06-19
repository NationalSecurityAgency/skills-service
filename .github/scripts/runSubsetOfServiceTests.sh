#!/bin/bash
usage() { echo "Usage runSubsetOfServiceTests.sh -c <current_run> -t <total_concurrent_runs>" 1>&2; exit 1; }

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
echo "Running subset of service tests"
echo "Current Run: $currentRun";
echo "Additional Vars: $additionalTestVars";
echo "Total Concurrent: $totalConcurrent";

cd ./src/test/java/
IFS=$'\n'
allTests=($(find . | grep -i -E '(Test[s]?|Spec[s]?|IT)\.(groovy|java)' | sed "s/\.\///g" | sed "s/\//\./g" | sed "s/\.groovy//g" | sed "s/\.java//g" | sort))
unset IFS
cd ../../../

totalNum=${#allTests[@]}
numPerRun=$((totalNum/totalConcurrent))
start=$(((currentRun-1)*numPerRun))
if  [ "$currentRun" -eq "$totalConcurrent" ]; then
    numPerRun=$totalNum
fi
testsInThisRun=${allTests[@]:$start:$numPerRun}
echo "There are [$totalNum] total tests. Run [$currentRun]: Start Number=[$start], running up to [$numPerRun] tests"

# this variable will be tracked to see if CachingSpec tests is in this run/batch
# CachingSpec requires dashboard and client-display projects build and deployed
cachingSpec='no'

# separate tests that end with IT into another array
# tests that end with IT must be executed with maven-failsafe-plugin
integrationTests=()
serviceTests=()
for testClass in ${testsInThisRun[@]}
do
  if [[ $testClass =~ .*IT$ ]]; then
    integrationTests+=($testClass)
  else
    serviceTests+=($testClass)
  fi
  if  [ "$testClass" == 'skills.intTests.CachingSpec' ]; then
    cachingSpec='yes'
  fi
done
echo "Running [${#serviceTests[@]}] service tests (Spec[s]?) and [${#integrationTests[@]}] integration (IT$) tests."

echo "Service tests:"
count=1;
for testClass in ${serviceTests[@]}
do
  echo "$count: $testClass"
  count=$((count+1))
done

if (( ${#integrationTests[@]} != 0 )); then
  echo "Integration tests:"
  count=1;
  for testClass in ${integrationTests[@]}
  do
    echo "$count: $testClass"
    count=$((count+1))
  done
fi

if  [ "$cachingSpec" == 'yes' ]; then
    echo 'Found CachingSpec. Building dashboard'
    cd ../dashboard-prime
    npm run deploy
#    cd ../client-display
#    npm install
#    npm run deploy
    cd ../service
fi

cd ../dashboard
npm run getDashboardVersion
cd ../service

IFS=,;
serviceTestsExecString="${serviceTests[*]}"
verifyExecString=""
if (( ${#integrationTests[@]} != 0 )); then
  integrationTestsString="${integrationTests[*]}"
  integrationTestsExecString="-Dit.test="$integrationTestsString""
  verifyExecString="verify"
fi
unset IFS

commandToRun="mvn ${additionalTestVars} -Dtest="${serviceTestsExecString}" ${integrationTestsExecString} test ${verifyExecString}"
echo $commandToRun
exec $commandToRun