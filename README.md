# xkcdl
A tiny downloader for xkcd.com

Saves the comics in subfolders containing 200 comics at once. <br>
Is pretty fast since it saves multiple comics in parallel to avoid bottlenecks and xkcd.com is nicely hosted.

## How to use

Just run the tool with the path to where you want your comics downloaded. <br>
Once you've done that, it will save a cfg file with the location specified.

For example:

    java -jar xkcdl.jar "/home/yourname/Pictures/XKCD"
   
