
if [ "$2" = "toxml" ]
then
  source="fastlane/metadata/android/en-US/$1"
  destination="build/$1.xml"
  echo '<?xml version="1.0" encoding="utf-8"?>' >$destination
  echo '<resources>' >>$destination
  while read line  || [ -n "$line" ]
  do
    key=`echo $line|cut  -f1 -d= | cut -d '"' -f2`
    value=`echo $line|cut  -f2 -d= | cut -d '"' -f2`
    if [ ! -z "$key" ]
    then
      echo '<string name="'$key'">'$value'</string>'  >>$destination
    fi
  done<$source
  echo '</resources>' >>$destination
  echo "\n\n"
  xmllint --format $destination
  if [ $? -ne 0 ]; then
    echo "\n\n\nFatal Error: XML is not properly formed. Please check strings file\n\n"
    exit 1
  fi
fi

if [ "$2" = "fromxml" ]
then
for source in fastlane/metadata/android/*/*.xml; do
  destination=`echo $source | sed 's/.xml//g'`
  echo "Processing $destination..."
  rm -rf $destination
  while read line  || [ -n "$line" ]
  do
    key=`echo $line | cut -f2 -d'"'`
    value=`echo $line | sed -n 's:.*<string.*>\(.*\)</string>.*:\1:p'`
    if [ ! -z "$value" ]
    then
      echo "\n" >>$destination
      echo '"'$key'" = "'$value'"' >>$destination
    fi
  done<$source
  rm $source
done
fi

if [ "$1" = "copy_screenshots" ]
then
  for destination in ../fastlane/metadata/android/*/images/phoneScreenshots; do
    cp ../fastlane/manual_taken_screenshots/phoneScreenshots/* "$destination/"
  done
fi

if [ "$1" = "delete_raw" ]
then
  for destination in ../fastlane/metadata/android/*/images/phoneScreenshots; do
    for entry in "$destination"/*
    do
      if [[ ! $entry == *"framed"* ]]; then
        echo "Removing $entry"
        rm $entry
      fi
    done
  done
fi