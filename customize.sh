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
if [ -f $VEN/build.prop ]; then BUILDS="/system/build.prop $VEN/build.prop"; else BUILDS="/system/build.prop"; fi

MIUI=$(grep "ro.miui.ui.version.*" $BUILDS)
if [ $MIUI ] && [ $API -lt "30" ]; then
  ui_print " MIUI 12 or lower is not supported"
  abort " Aborting..."
fi

ui_print "- Extracting module files"

unzip -o "$ZIPFILE" 'overlays/*' 'system/*' 'common/*' 'module.prop' 'system.prop' 'sepolicy.rule' 'zipsigner*' 'uninstall.sh' 'quickswitch' 'service.sh' 'webroot/*' -d $MODPATH >&2
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
  if ( [ -n "$KSU" ] && [ -e "/data/adb/ksu/modules.img" ] ) || ( [ -n "$APATCH" ] && [ -z "$APATCH_BIND_MOUNT" ] ) ; then
    sed -i "/MAGIC_MOUNT=true*/d" $MODPATH/quickswitch
  fi
else
  ln -s /data/adb/magisk/busybox $MODPATH/busybox
fi


########################################
# APK INSTALL WITH AUTO RETRY SYSTEM  #
########################################

if [ -z "$NOAPK" ]; then

  ui_print "- Preparing QuickSwitch.apk"
  unzip -o "$ZIPFILE" 'QuickSwitch.apk' -d /data/local/tmp >&2

  ui_print "- Installing QuickSwitch.apk (Attempt 1)"
  pm install -r "/data/local/tmp/QuickSwitch.apk"
  INSTALL_RESULT=$?

  if [ $INSTALL_RESULT -ne 0 ]; then

    ui_print " "
    ui_print " ! First install failed"
    ui_print " ! Cleaning old installation..."
    ui_print " "

    # Remove installed package if exists
    pm uninstall xyz.paphonb.quickswitch >/dev/null 2>&1

    # Remove temp apk
    rm -rf /data/local/tmp/QuickSwitch.apk

    # Extract again clean
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


rm -rf /data/adb/modules/quickstepswitcher

if [ -d $MODULEDIR ]; then
  if [ $MODVER -ge 3300 ]; then
    ui_print "- Module updating - retaining current provider"
    for i in $(find $MODULEDIR/system/* -type d -maxdepth 0); do
      cp -rf "$i" $MODPATH/system/
    done
  else
    for i in $(find $MODULEDIR/* -maxdepth 0 | sed "/^module.prop/ d"); do
      rm -rf "$i"
    done
    ui_print "- Major upgrade! clearing out all old files and directories."
  fi
fi

set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/aapt2 2000 2000 0755
set_perm $MODPATH/busybox 2000 2000 0755
set_perm $MODPATH/quickswitch 2000 2000 0777
set_perm $MODPATH/zipsigner 0 0 0755
set_perm $MODPATH/zipsigner-3.0-dexed.jar 0 0 0644
#Test