# iTunes-to-Android
Java program that syncs your iTunes playlists to your Android device over USB. Requires ADB: https://developer.android.com/studio/command-line/adb.html

Program.jar is the only file here you need to download to make it work. You can rename it to whatever you want. The source is included here for those interested.

I know this program might be redundant for some, as most Android devices already have ways to do this. However, mine (Samsung Galaxy S6) does not. I did not trust any of the results on Google that claimed to be able to do this, so I made my own.

This program is untested with any operating system and device other than Microsoft Windows 7 64-bit and a Sprint Samsung Galaxy S6. Support for other OSs and devices is contingent on if/how ADB functions for them.

## Usage
Before you can use the program, you must install ADB on your computer (linked above). This should add it to PATH as well (which is currently required), but if it does not, see the third link below. You will also need to enable USB debugging on your device. The link above should also provide you with the steps to do that. If you don't want to install the whole Android SDK just to get ADB - and you probably shouldn't, as the SDK is quite large and useless if you aren't a developer - these links may be of use as well:
* https://www.androidcentral.com/get-adb-and-fastboot-utilities-without-installing-sdk-directly-google - An article with links to the platform-tools of the SDK, which is considerably smaller than the entire Android SDK. Includes more than just ADB but is still only a few megabytes. The platform-tools directory inside the archive you download will need to be added to PATH once you extract it.
* http://adbshell.com/upload/adb.zip - An even smaller ZIP file containing only adb.exe and the DLLs it requires, for Microsoft Windows. The directory you extract this to will need to be added to PATH.
* https://www.java.com/en/download/help/path.xml - Basic instructions on how to edit your PATH variable to include the directory that contains ADB, which is necessary if you use either of the two above links.

When you load up the program you should see a Setup tab. This tab will allow you to select your iTunes music library XML file, which is usually in the My Music\iTunes\ folder. You will be able to view your full library in the program if it successfully loads (could take a few seconds depending on the size of the library).

Below that, there should be a status message indicating the status of your Android device connection. If it is connected, it should say so. However, occasionally it may say that it cannot find a device even after it has loaded your device, due to some quirks with ADB. If the Sync tab appears to be working, then you can ignore the connection status message.

Select the playlists you want to copy over on the Sync tab. Ctrl-click to select multiple playlists. Then click the Build File List From Playlists button. If you have your device successfully connected, this should populate both lists on the right side of the window. One list is for songs that are not in your device music folder and need to be copied. The other list is for extra files found in your device music folder that are not in your selected iTunes playlists. From there it should be self-explanatory. It won't create playlists on your device, it will only copy all the files over and/or delete leftover files that are not on your selected iTunes playlists. It will keep the same relative paths as your iTunes music folder. As in, if your iTunes music folder is G:\Music\, then all files and directories with iTunes songs on your selected playlists within that folder will be copied to /sdcard/Music/ on your device. If there are songs on your iTunes playlists that are not in your standard iTunes music folder, you will be copied as well, but the file path will be built from the song artist and album.

## Status Apr 10, 2018
Only works if your Android device has an /sdcard/Music folder, and you want your music to be in that folder. There are probably also many quirks that might prevent it from working regardless, as I made this specifically for my one device and originally never intended to do anything beyond that. The program currently offers very little messaging to indicate if an error occurs or not, unless you launch it with the console visible. This will also not work yet if you have multiple android devices connected (this includes any emulators you may be using).