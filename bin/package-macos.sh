cd build/libs
unzip $1.jar -d $1

find . -name "*.*lib" | xargs codesign -vvv \
  --force \
  --options runtime \
  --timestamp \
  -s "Developer ID Application: $MAC_SIGNING_KEY_USER_NAME ($NOTARY_TOOL_TEAM_ID)"

rm $1.jar

cd $1
zip -r ../$1.jar .

cd ..
rm -Rf $1

cd ../..

jpackage --name $2 \
  --input build/libs \
  --main-jar $1.jar \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --app-version $3 \
  --mac-sign \
  --mac-signing-key-user-name "$MAC_SIGNING_KEY_USER_NAME"

xcrun notarytool submit \
  --apple-id $NOTARY_TOOL_APPLE_ID \
  --team-id $NOTARY_TOOL_TEAM_ID \
  --password $NOTARY_TOOL_PASSWORD \
  --wait $2-$3.dmg

xcrun stapler staple $2-$3.dmg
