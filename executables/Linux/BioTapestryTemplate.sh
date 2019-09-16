#!/bin/bash

JARPATH=`dirname $0`
JAVA_HOME=${JARPATH}/lib/__WJRL_JDK_VER__
$JAVA_HOME/bin/java -Xmx8000m -jar ${JARPATH}/lib/sBioTapestry-V__WJRL_VERNUM__.jar org.systemsbiology.biotapestry.app.EditorApplication $*

