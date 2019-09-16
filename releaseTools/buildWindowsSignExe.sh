# /bin/bash

#
# This script signs the BioTapestry executable file:

EXEHOME=$1
SC_HOME=$2
KEY_HOME=$3

PEMFILE=`ls ${KEY_HOME}/*-SHA2.pem`

mv ${EXEHOME}/BioTapestry.exe ${EXEHOME}/UnsignedBioTapestry.exe

#
# This requires that the osslsigncode tool (https://sourceforge.net/projects/osslsigncode/) has been installed:
#

${SC_HOME}/osslsigncode sign -certs ${PEMFILE} -key ${KEY_HOME}/ISB_codesign.key -n "BioTapestry" \
  -i http://www.BioTapestry.org -t http://timestamp.verisign.com/scripts/timstamp.dll -in ${EXEHOME}/UnsignedBioTapestry.exe -out ${EXEHOME}/BioTapestry.exe
