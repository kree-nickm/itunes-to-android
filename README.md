# iTunes-to-Android
Java program that syncs your iTunes playlists to your Android device over USB. Requires USB debugging enabled on your device as well as Android Debug Bridge (ADB): https://developer.android.com/studio/command-line/adb.html

Program.jar is the only file here you need to download to make it work. You can rename it to whatever you want. The source is included here for those interested.

I know this program might be redundant for some, as most Android devices already have ways to do this. However, mine does not. I did not trust any of the results on Google that claimed to be able to do this, so I made my own.

This program is untested with any operating system and device other than Microsoft Windows 10 64-bit and a Samsung Galaxy S9. Support for other OSs and devices is contingent on if/how ADB functions for them.

## Usage
Before you can use the program:
* You must have ADB, which can be downloaded by exploring the link above. The ADB file(s) must either be in the same folder as Program.jar, or ADB's folder must be added to PATH. I will also include the minimum required files in the download.
* USB debugging must be enabled on your device. The link above should also provide you with the steps to do that.
* Verify that your device is recognized by your computer when you connect it via USB. If not, you probably need to install some Android USB drivers.

When you load up the program you should see a Setup tab. This tab will allow you to select your iTunes music library XML file, which is usually in the My Music\iTunes\ folder. You will be able to view your full library in the program if it successfully loads (could take a few seconds depending on the size of the library).

Below that, you can select your connected Android device, or Scan for newly plugged-in/authorized devices. If your device says "unauthorized" or does not appear, then you may need to authorize your computer to access the device via USB debugging. Usually the device will give you a popup asking permissions, if you have USB debugging enabled.

Below that, you can browse through the folders on your phone to find the one where you want your music files to be located. Select it, then click Load right above the directory tree.

Select the playlists you want to copy over on the Sync tab. Ctrl-click to select multiple playlists. Then click the Build File List From Playlists button. If you have your device successfully connected, this should populate both lists on the right side of the window. One list is for songs that are not in your device music folder and need to be copied. The other list is for extra files found in your device music folder that are not in your selected iTunes playlists.

From there it should be self-explanatory. It will copy all the files over and/or delete leftover files that are not on your selected iTunes playlists, as well as create playlist files that you can import into your music player. It will keep the same relative paths as your iTunes music folder. If there are songs on your iTunes playlists that are not in your standard iTunes music folder, they will be copied as well, but the file path will be built from the song artist and album.
