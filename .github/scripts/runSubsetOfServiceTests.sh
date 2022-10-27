#!/bin/bash
echo "Running subset of service tests"

usage() { echo "Usage runSubsetOfServiceTests.sh -c <current_run> -t <total_concurrent_runs>" 1>&2; exit 1; }

while getopts ":c:t:" flag;
do
    case "${flag}" in
        c)
          currentRun=${OPTARG}
          ;;
        t)
          totalConcurrent=${OPTARG}
          ;;
        *)
          usage
          ;;
    esac
done
echo "Current Run: $currentRun";
echo "Total Concurrent: $totalConcurrent";
if [ -z "${currentRun}" ] || [ -z "${totalConcurrent}" ]; then
    usage
fi


cd ./src/test/java/
IFS=$'\n'
allTests=($(find . | grep -E '(Test[s]?|Spec[s]?)\.(groovy|java)' | sed "s/\.\///g" | sed "s/\//\./g" | sed "s/\.groovy//g" | sed "s/\.java//g" | sort))
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
for ELEMENT in ${smallArray[@]}
do
  echo "$count: $ELEMENT"
  count=$((count+1))
done

batchStr=${smallArray[*]// /,}
commandToRun="mvn -Dtest="$batchStr" test"
echo $commandToRun
exec $commandToRun

