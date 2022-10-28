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
smallArray=${allTests[@]:$start:$numPerRun}
echo "There are [$totalNum] total tests. Run [$currentRun]: Start Number=[$start], running up to [$numPerRun] tests"
echo "Running these tests:"
count=1;
cachingSpec='no'
for testClass in ${smallArray[@]}
do
  echo "$count: $testClass"
  count=$((count+1))
  if  [ "$testClass" == 'skills.intTests.CachingSpec' ]; then
    cachingSpec='yes'
  fi
done

if  [ "$cachingSpec" == 'yes' ]; then
    echo 'Found CachingSpec. Building client-display...'
    cd ../client-display
    npm install
    npm run deploy
    cd ../service
fi

cd ../dashboard
npm run getDashboardVersion
cd ../service

batchStr=${smallArray[*]// /,}
commandToRun="mvn ${additionalTestVars} -Dtest="${batchStr}" test"
echo $commandToRun
exec $commandToRun

