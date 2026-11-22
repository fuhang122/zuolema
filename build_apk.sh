#!/bin/bash
set -e
cd /root/workspace/zuolema_android
AJ=/usr/lib/android-sdk/platforms/android-23/android.jar
D8=/opt/bt34/android-14/d8
rm -rf gen res.zip base.apk classes dexout withdex.apk 做了吗.apk 做了吗.apk.idsig
mkdir -p gen classes dexout

/usr/bin/aapt2 compile --dir res -o res.zip
/usr/bin/aapt2 link -o base.apk -I "$AJ" --manifest AndroidManifest.xml -R res.zip --auto-add-overlay \
  --java gen -A assets --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 1 --version-name 1.0.0

javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" -d classes \
  $(find java gen -name '*.java')
"$D8" --lib "$AJ" --min-api 26 --output dexout $(find classes -name '*.class')

python3 - <<'PY'
import zipfile
z = zipfile.ZipFile('base.apk', 'a', zipfile.ZIP_DEFLATED)
z.write('dexout/classes.dex', 'classes.dex')
z.close()
PY

/usr/bin/zipalign -f -p 4 base.apk withdex.apk
/usr/bin/apksigner sign --ks ks.jks --ks-pass pass:dsh123456 --key-pass pass:dsh123456 \
  --out 做了吗.apk withdex.apk
/usr/bin/apksigner verify --print-certs 做了吗.apk | head -3
ls -lh 做了吗.apk
