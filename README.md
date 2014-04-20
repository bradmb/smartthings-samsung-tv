SmartThings: Samsung TV Remote
=============================

**Allows controlling Samsung Smart TVs using SmartThings**

This currently only allows you to trigger actions on your TV if SmartThings detects a mode change (or you press the app). You can, of course, modify this to support other actions. The end goal is to add the TV as an actual Device on your Things page, but I haven't  yet been able to figure out how to make LAN calls from a device (the commands you can run in an app don't appear to work on a device).

**Requirements:**
This has only been tested on a Samsung UN50EH5300 TV, so I can't confirm that it will work on other devices. If your Samsung TV has AllShare support, it has a good chance of working.

**Before Starting:**
To properly configure this application, you will need to know your device's IP and MAC Address. This information should be viewable from your TV under Menu > Network > Network Status.

**To Use:**

1. Create a new SmartApp (https://graph.api.smartthings.com/ide/app/create)

2. Paste the code from *samsung.smartapp.groovy* into the IDE

3. Publish it for yourself

4. Install from the "My Apps" section via your SmartThings application

**Please note:** If everything goes right, Your TV will prompt you to authorize "SmartThings" to interact with it. If you see this message, make sure to allow this access. If you don't see the message, go back and review your app configuration.
