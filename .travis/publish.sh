#!/bin/bash
if git config --get remote.origin.url 2>&1 | grep -F github.com/zhanhb/ckfinder-spring-boot -q && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  mvn -Duser.name=zhanhb -DskipTests=true clean deploy -s .travis/settings.xml $*
else
  mvn install -B
fi

