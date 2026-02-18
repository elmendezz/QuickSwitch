# What are the differences in this fork?
- QuickSwitch replaced with an useful app
- No more slow web UI's! (You used to have to click on your web UI app, then on launchers, and then set them up.)
- Easy-to-use app: click on your preferred launcher and you're done
- You can see the logs!
- lightweight app

# QuickSwitch fork  

This is a fork of QuickSwitch.  
QuickSwitch is a Magisk/KernelSU/Apatch module which systemlessly allows supported launchers to access the recents (QuickStep) APIs.  

To learn about the original, see [here](https://github.com/skittles9823/QuickSwitch).  
Also, please do not report bugs in forked versions upstream.  

## Requirements:

- Latest version of Magisk or KernelSU or Apatch  
- Android 9+  

## ⚠Warning⚠  
This module operates the system using Root privileges.  
Please use it with extreme caution.  
Also, many users in the Telegram community have encountered trouble without reading the Readme.  
Please read the Readme carefully before using this module.  

This is not limited to this module,  
When you operate the system, you need to understand how to use it and manage the risk.  

## Installation:  

## Magisk/KSU/APatch:  
1. Install the latest QuickSwitch zip from the [GitHub releases](https://github.com/j7b3y/QuickSwitch/releases/latest).  
2. `su -c /data/adb/modules/quickswitch/quickswitch --ch=launcher.package.name`  
3. Reboot.  
4. Verify your new recents provider is correct.  
5. Set the new recents provider as the default launcher.  
6. Profit.  

※ Please run it in a terminal application (termux, etc.) with root access.  
※ Replace launcher.package.name with the name of your launcher. For example app.lawnchair.debug.  

## Web Interface  
This is not complete but may help users who are having trouble with command line operations.  
If you are a magisk user, you will need the following apps.  
- [KsuWebUIStandalone](https://github.com/5ec1cff/KsuWebUIStandalone)  

## Support:
If you encounter any problems, you may find some hints in the github issues or [Telegram Group](https://t.me/QuickstepSwitcherSupport).  

## Branches  
- master  
Original source.  
- anyfix/*  
It was to be used for pull requests.  
- WIP/*  
With incomplete new elements.