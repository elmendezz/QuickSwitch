# @Skittles9823 made this ascii and is way to proud of it
ui_print " "
ui_print "       _____       "
ui_print "   __ |     | __   "
ui_print "  |  ||     ||  |  "
ui_print "  |  ||     ||  |  "
ui_print "  |__||     ||__|  "
ui_print "      |_____|      "
ui_print "    QuickSwitch    "
ui_print "                   "
ui_print " Fork by elmendezz "
ui_print " "

[ $BOOTMODE == "false" ] && abort "Installation failed! QuickSwitch must be installed via Magisk/KernelSU Manager!"
[ $API -lt "28" ] && abort "QuickSwitch is for Android Pie+ only"

VEN=/system/vendor
[ -L /system/vendor ] && VEN=/vendor
if [ -f $VEN/build.prop ]; then 
  BUILDS="/system/build.prop $VEN/build.prop"
else 
  BUILDS="/system/build.prop"
fi

MIUI=$(grep "ro.miui.ui.version.*" $BUILDS)
if [ "$MIUI" ] && [ $API -lt "30" ]; then
  ui_print " MIUI 12 or lower is not supported"
  abort " Aborting..."
fi

ui_print "- Extracting module files"

unzip -o "$ZIPFILE" \
'overlays/*' \
'system/*' \
'common/*' \
'module.prop' \
'system.prop' \
'sepolicy.rule' \
'zipsigner*' \
'uninstall.sh' \
'quickswitch' \
'service.sh' \
'webroot/*' \
-d $MODPATH >&2

chmod +x $MODPATH/common/*

AAPT2=aapt2_$(getprop ro.product.cpu.abi)
cp -af $MODPATH/common/$AAPT2 $MODPATH/aapt2 || abort "Unsupported Arch!"
rm -rf $MODPATH/common

rm -rf /data/adb/service.d/quickswitch.sh
rm -rf /data/adb/service.d/quickswitch-service.sh
rm -rf /data/adb/post-fs-data.d/quickswitch-post.sh

rm -rf /data/resource-cache/overlays.list
find /data/resource-cache/ -name "*QuickstepSwitcherOverlay*" -exec rm -rf {} \;
find /data/resource-cache/ -name "*QuickSwitchOverlay*" -exec rm -rf {} \;

MODULEDIR="/data/adb/modules/$MODID"
MODVER=$(grep_prop versionCode $MODULEDIR/module.prop)

# Root solution checks
if [ -z "$KSU" ]; then
  sed -i "/KSU=true*/d" $MODPATH/quickswitch
fi

if [ -z "$APATCH" ]; then
  sed -i "/APATCH=true*/d" $MODPATH/quickswitch
fi

if [ -n "$KSU" ] || [ -n "$APATCH" ]; then
  NOAPK=true
  ln -s $(which busybox) $MODPATH/busybox
  if ( [ -n "$KSU" ] && [ -e "/data/adb/ksu/modules.img" ] ) || \
     ( [ -n "$APATCH" ] && [ -z "$APATCH_BIND_MOUNT" ] ) ; then
    sed -i "/MAGIC_MOUNT=true*/d" $MODPATH/quickswitch
  fi
else
  ln -s /data/adb/magisk/busybox $MODPATH/busybox
fi

############################################
# APK INSTALL WITH RETRY + SAFE FALLBACK
############################################

if [ -z "$NOAPK" ]; then

  ui_print "- Preparing QuickSwitch.apk"
  unzip -o "$ZIPFILE" 'QuickSwitch.apk' -d /data/local/tmp >&2

  ui_print "- Installing QuickSwitch.apk"
  pm install -r "/data/local/tmp/QuickSwitch.apk"
  INSTALL_RESULT=$?

  if [ $INSTALL_RESULT -ne 0 ]; then

    ui_print " "
    ui_print " ! First install failed"
    ui_print " ! Cleaning old installation..."
    ui_print " "

    pm uninstall com.elmendezz.qsre >/dev/null 2>&1
    rm -rf /data/local/tmp/QuickSwitch.apk

    unzip -o "$ZIPFILE" 'QuickSwitch.apk' -d /data/local/tmp >&2

    ui_print "- Reinstalling QuickSwitch.apk (Attempt 2)"
    pm install -r "/data/local/tmp/QuickSwitch.apk"
    INSTALL_RESULT=$?

    if [ $INSTALL_RESULT -ne 0 ]; then
      ui_print " "
      ui_print " !! Second install failed"
      ui_print " !! Switching to module-only mode (SKIPUNZIP=1)"
      ui_print " "

      rm -rf /data/local/tmp/QuickSwitch.apk
      SKIPUNZIP=1
      NOAPK=true
    else
      ui_print "- APK installed successfully on retry"
      rm -rf /data/local/tmp/QuickSwitch.apk
    fi

  else
    ui_print "- APK installed successfully"
    rm -rf /data/local/tmp/QuickSwitch.apk
  fi
fi

############################################
# FORCE RETAIN LAUNCHER PROVIDER
############################################

rm -rf /data/adb/modules/quickstepswitcher

if [ -d "$MODULEDIR" ]; then

  CURRENT_OVERLAY_BACKUP="/data/local/tmp/qs_overlay_backup"

  # Backup existing launcher overlay
  if [ -d "$MODULEDIR/system/product/overlay" ]; then
    ui_print "- Backing up current launcher provider..."
    rm -rf "$CURRENT_OVERLAY_BACKUP"
    mkdir -p "$CURRENT_OVERLAY_BACKUP"
    cp -rf "$MODULEDIR/system/product/overlay/"* "$CURRENT_OVERLAY_BACKUP/" 2>/dev/null
  fi

  if [ "$MODVER" -ge 3300 ]; then
    ui_print "- Module updating - retaining current provider"
    for i in $(find $MODULEDIR/system/* -type d -maxdepth 0); do
      cp -rf "$i" $MODPATH/system/
    done
  else
    ui_print "- Major upgrade detected!"
    ui_print "- Cleaning old files..."
    for i in $(find $MODULEDIR/* -maxdepth 0 | sed "/^module.prop/ d"); do
      rm -rf "$i"
    done
  fi

  # Restore launcher overlay
  if [ -d "$CURRENT_OVERLAY_BACKUP" ]; then
    ui_print "- Restoring previous launcher provider..."
    mkdir -p $MODPATH/system/product/overlay
    cp -rf "$CURRENT_OVERLAY_BACKUP/"* $MODPATH/system/product/overlay/ 2>/dev/null
    rm -rf "$CURRENT_OVERLAY_BACKUP"
    ui_print "- Launcher provider restored successfully"
  fi

fi

############################################
# PERMISSIONS
############################################

set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/aapt2 2000 2000 0755
set_perm $MODPATH/busybox 2000 2000 0755
set_perm $MODPATH/quickswitch 2000 2000 0777
set_perm $MODPATH/zipsigner 0 0 0755
set_perm $MODPATH/zipsigner-3.0-dexed.jar 0 0 0644

ui_print " "
ui_print " ✔ QuickSwitch installation completed"
ui_print " ✔ Launcher provider preserved"
ui_print " "