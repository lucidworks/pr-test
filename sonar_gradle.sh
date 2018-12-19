#!/bin/bash

# cleanup
rm -rf sonarqube_results
rm -rf .scannerwork
# ./gradlew --stop  # if the job terminates, gradle doesn’t always play nice. Leaves incompatible workers behind

# clean and build, only once
./gradlew clean build

# prepare envvars for sonar to use
export GIT_BRANCH=$(git branch | grep \* | cut -d ' ' -f2)
if [ "$GIT_BRANCH" == "master" ]; then
    export TARGET_BRANCH=""
elif [ "$GIT_BRANCH" == "develop" ]; then
    export TARGET_BRANCH="master"
else
    export TARGET_BRANCH="develop"
fi
export VERSION=$(grep "^version=" gradle.properties | cut -d'=' -f2-)

# run sonar scan
#  use -Dsonar.analysis.mode=preview to run scan w/o pushing to server

echo "Running Sonar with the following params:"
echo "GIT_BRANCH    : $GIT_BRANCH"
echo "TARGET_BRANCH : $TARGET_BRANCH"
echo "VERSION       : $VERSION"

./gradlew sonarqube # -Dsonar.analysis.mode=preview # see INFRA-1637
