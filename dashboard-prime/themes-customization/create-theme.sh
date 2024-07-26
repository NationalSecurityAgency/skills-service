#!/bin/bash
#
# Copyright 2024 SkillTree
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


sassThemeProj="primevue-sass-theme"
work_dir=../../../
currentDir=$(pwd)

get_primevue_version() {
  local dependency="primevue"
  local version=$(jq -r ".dependencies.\""$dependency"\"" ../package.json)
  echo $version
}

handle_theme() {
  themeName=$1
  themePath=$2

  themeFile=$(realpath "./${themeName}/theme-overrides.scss")
  appendCssFile=$(realpath "./${themeName}/theme-append.css")
  version=$(get_primevue_version)
  echo "Version of primevue: [$version]"
  echo `ls $work_dir`


  primeVueSass=$(realpath "$work_dir/${sassThemeProj}")
  if [ -d "$primeVueSass" ]; then
    echo "[$primeVueSass] already exist, no need to check out"
  else
    echo "[$primeVueSass] does not exist, will clone [$sassThemeProj] repo"
    # todo: clone
  fi

  cd "$primeVueSass" && eval "git checkout $version"

  echo "Theme file: [$themeFile]"
  themeDestToReplace="$primeVueSass/$themePath/theme.scss"
  if [ -f "$themeFile" ]; then
    while IFS=: read -r key value; do
      if [ -n "$key" ] && [ -n "$value" ]; then
        if grep -q "$key" "$themeDestToReplace"; then
          echo "Placing key [$key] with value [$value]"
          # Replace the value after the key with the new value
          sed -i "s/$key: .*;/$key: $value;/" "$themeDestToReplace"
        else
          echo "Key '$key' does not exist in the file. appending!"
          valueToAppend="$key: $value"
          echo "$valueToAppend" | cat - "$themeDestToReplace" > temp && mv temp "$themeDestToReplace"
        fi
      fi
    done < "$themeFile"
  else
    echo "Theme file does not exist: [$themeFile]"
  fi

#  echo >> $themePath/theme.scss # add new line
#  cat $appendCssFile >> $themePath/theme.scss

  cd "$primeVueSass" && eval "sass --update $themePath/theme.scss:$themePath/theme.css"

  cd $currentDir
  cp $primeVueSass/$themePath/theme.css ../public/themes/${themeName}/theme.css
  echo "Copied theme file: [$primeVueSass/$themePath/theme.css] to [../public/themes/${themeName}/theme.css]"
}

handle_theme "skills-light-green" "themes/lara/lara-light/green"

cd $currentDir
handle_theme "skills-dark-green" "themes/lara/lara-dark/green"
