cd module && zip -r ../QuickSwitch-fork.zip $(cat ../files.txt)
cd ../withoutui && zip -r -u ../QuickSwitch-fork.zip module.prop

cd ../module && zip -r ../QuickSwitch-fork-webui $(cat ../files.txt)
cd ../webui && zip -r -u ../QuickSwitch-fork-webui module.prop webroot*