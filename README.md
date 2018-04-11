# iTunes-to-Android
Java program that syncs your iTunes playlists to your Android device over USB. Requires ADB: https://developer.android.com/studio/command-line/adb.html

Program.jar is the only file here you need to download to make it work. You can rename it to whatever you want. The source is included here for those interested.

I know this program might be redundant for some, as most Android devices already have ways to do this. However, mine (Galaxy S6) does not. I did not trust any of the results on Google that claimed to be able to do this, so I made my own.

## Usage
Before you can use the program, you must install ADB on your computer (linked above). This should add it to PATH as well, but if it does not, you will have to do that maunally. That is beyond the scope of this guide. You will also need to enable USB debugging on your device. The link above should also provide you with the steps to do that.

When you load up the program you should see a Setup tab. This tab will allow you to select your iTunes music library XML file, which is usually in the My Music\iTunes\ folder. You will be able to view your full library in the program if it successfully loads (could take a few seconds depending on the size of the library).

Below that, there should be a status message indicating the status of your Android device connection. If it is connected, it should say so. However, occasionally it may say that it cannot find a device even after it has loaded your device, due to some quirks with ADB. If the Sync tab appears to be working, then you can ignore the connection status message.

Select the playlists you want to copy over on the Sync tab. Ctrl-click to select multiple. Then click the Build File List From Playlists button. If you have your device successfully connected, this should populate both lists on the right side of the window. From there it should be self-explanatory. It won't create playlists on your device, it will only copy all the files over. It will keep the same relative paths as your iTunes music folder. As in, if your iTunes music folder is G:\Music\, then all files and directories with iTunes songs on your selected playlists within that folder will be copied to /sdcard/Music/ on your device. If there are songs on your iTunes playlists that are not in your standard iTunes music folder, you will be copied as well, but the file path will be built from the song artist and album.

## Status Apr 10, 2018
Only works if your Android device has an /sdcard/Music folder, and you want your music to be in that folder. There are probably also many quirks that might prevent it from working regardless, as I made this specifically for my one device and originally never intended to do anything beyond that. The program currently offers very little messaging to indicate if an error occurs or not, unless you launch it with the console visible.