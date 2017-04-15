#!/bin/bash

source bin/commons.sh

#
# Make archive for the release
function makeArchive(){
    VERSION=$1
    mkdir choco-${VERSION} || quit "unable to make choco-${VERSION}"
    git checkout ${VERSION} || quit "unable to checkout ${VERSION}"
    mvn clean install -DskipTests || quit "unable to install"

    mv ./target/choco-solver-${VERSION}.jar ./choco-${VERSION} || quit "unable to mv jar"
    mv ./target/choco-solver-${VERSION}-with-dependencies.jar ./choco-${VERSION}|| quit "unable to mv dep"
    mv ./target/choco-solver-${VERSION}-sources.jar ./choco-${VERSION}|| quit "unable to mv src"

    cp ./user_guide.pdf ./choco-${VERSION}/user_guide-${VERSION}.pdf|| quit "unable to cp user guide"
    cp ./README.md ./choco-${VERSION}|| quit "unable to cp README"
    cp ./CHANGES.md ./choco-${VERSION}|| quit "unable to cp CHANGES"
    cp ./LICENSE ./choco-${VERSION}|| quit "unable to cp LICENSE"

    mvn javadoc:aggregate  || quit "unable to javadoc"
    cd target/site/|| quit "unable to cd target/site"
    zip -r apidocs-${VERSION}.zip ./apidocs/*|| quit "unable to zip apidocs"
    mv apidocs-${VERSION}.zip ../../choco-${VERSION}|| quit "unable to mv apidocs"
    cd ../../|| quit "unable to cd ../../"

    zip choco-${VERSION}.zip ./choco-${VERSION}/*|| quit "unable to zip dir"
}
#
# push javadoc onto website
function pushJavadoc(){
    VERSION=$1
    LOCAL=`mktemp -d -t choco.XXX`|| quit "unable to make temp dir"
    echo ${LOCAL}
    git -C ${LOCAL} init
    git -C ${LOCAL} remote add origin "https://github.com/chocoteam/chocoteam.github.io.git"||exit 1
    git -C ${LOCAL} fetch origin||exit 1
    git -C ${LOCAL} checkout master||git -C ${LOCAL} checkout -b master
    unzip -o ./choco-${VERSION}/apidocs-${VERSION}.zip -d ${LOCAL}|| quit "unable to unzip apidocs"
    cd ${LOCAL}|| quit "unable to go into website"
    git add ./apidocs/ || quit "unable to add apidocs"
    git commit -a -m "push javadoc of ${VERSION}"|| quit "unable to commit"
    git push origin master || quit "unable to push website"
    cd -
    rm -rf ${LOCAL} || quit "unable to remove ${LOCAL}"
}
#
# extract comment from CHANGES.md
function extractReleaseComment(){
    VERSION=$1
    LINES=$(grep -ne "-------------------" CHANGES.md |  cut -d : -f1 | tr "\n" ",")
    IFS=',' read -r -a ARRAY <<< "$LINES"
    FROM=$(expr ${ARRAY[1]} + 1)
    TO=$(expr ${ARRAY[2]} - 2)
    CHANGES=$(cat ./CHANGES.md | sed -n "${FROM},${TO}p" | perl -p -e 's/  /\\t/' | perl -p -e 's/(\#)([0-9])/\\#\2/' | perl -p -e 's/\n/\\n/g')
    echo '{"tag_name":'\""${VERSION}"\"',"target_commitish":"master","name":'\""${VERSION}"\"',"body":'\""$CHANGES"\"',"draft": false,"prerelease": false}' > $2
}

if [ "$#" -ne 1 ] ; then
    quit "Incorrect number of arguments"
fi


VERSION=$1

GH_API="https://api.github.com/repos/chocoteam/choco-solver/"
GH_UPL="https://uploads.github.com/repos/chocoteam/choco-solver/"

AUTH="Authorization: token ${GH_TOKEN}"

# Validate token.
curl -o /dev/null -i -sH "${AUTH}" "${GH_API}/releases" || quit "Error: Invalid repo, token or network issue!";

# create archive
makeArchive ${VERSION}

# upload javadoc to webpages
pushJavadoc ${VERSION}

# prepare release comment
#find position of release separator in CHANGES.md, only keep the 2nd and 3rd
temp_file="tmpreadme.txt"
$(touch ${temp_file}) || quit "Unable to create tmp file"

# extract release comment
extractReleaseComment ${VERSION} ${temp_file} || quit "Unable to extract release comment"

# create release
response=$(curl -i -sH "$AUTH" --data @${temp_file} "${GH_API}/releases") || quit "Unable to create the release"

# get the asset id
eval $(echo "$response" | grep -m 1 "id.:" | tr : = | tr -cd '[[:alnum:]]=')
[ "$id" ] || quit "Error: Failed to get release id for tag: ${VERSION}"; echo "$response" | awk 'length($0)<100' >&2

# add asset
curl -i -sH "$AUTH" -H "Content-Type: application/zip" \
         -data-binary @choco-${VERSION}.zip \
         "${GH_UPL}/releases/${VERSION}/assets?name=choco-${VERSION}.zip" \
         || quit "Unable to add asset"


# create the next milestone
NEXT=$(echo "${VERSION%.*}.$((${VERSION##*.}+1))") || quit "Unable to get next release number"
curl -i -sH "$AUTH" --data '{ "title": '\""${NEXT}"\"'}' \
        "${GH_API}milestones"

#createMilestone ${VERSION} ${GH_TOKEN}

rmdir -R choco-${VERSION}
rm ${temp_file} || quit "Unable to remove tmp file"
git checkout master
