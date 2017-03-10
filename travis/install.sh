#!/bin/bash
#
#    Copyright 2009-2016 the original author or authors.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

if [ $TRAVIS_JDK_VERSION == "openjdk6" ]; then
  # Java 1.6
  mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V -Pjava16
elif [ $TRAVIS_JDK_VERSION == "oraclejdk7" ] || [ $TRAVIS_JDK_VERSION == "openjdk7" ]; then
  # Java 1.7
  mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V -Pjava17
else
  # Java 1.8
  mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V
fi
