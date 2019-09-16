# /bin/bash

LICENSEDIR=$1
INSTALLER_HOME=$2
RESHOME=$3
CURRYEAR=$4

cd $INSTALLER_HOME
rm -rf $INSTALLER_HOME/Licenses
mkdir $INSTALLER_HOME/Licenses
cat $RESHOME/../LICENSE-README-TEMPLATE.txt | sed "s#__WJRL_CURRYEAR__#$CURRYEAR#" > Licenses/LICENSE-README.txt
cp $LICENSEDIR/licenses/*.txt $EXEHOME/Licenses
find $INSTALLER_HOME/Licenses -name .DS_Store -delete








