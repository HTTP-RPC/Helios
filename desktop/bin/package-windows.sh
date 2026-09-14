jpackage --name $2 \
  --input build/libs \
  --main-jar $1.jar \
  --app-version $3 \
  --icon $2.ico \
  --win-shortcut \
  --win-menu \
  --win-menu-group "Nova Power Cloud Solutions"
