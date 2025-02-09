# TOGoS's Minecraft Map Renderer

Command-line Java application for generating top-down images of Minecraft worlds, one region at a time.
Can automatically skip regeneration of tiles whose corresponding region has not been updated, based on file modification time.

![tile 0 0 8bit](https://github.com/user-attachments/assets/de895cf1-f939-4f65-b72b-2ac844030b3b)

## Basic Usage

```sh
java -jar TMCMR.jar region-directory -o output-directory
```

This will generate a PNG file for each region into the output directory along with tiles.html, which references all tiles in an HTML table so you can view the entire map.

If you run it again for the same regions and outputs, only regions that have been modified since their corresponding PNG files were generated (according to the filesystem timestamp) will be re-rendered.

Run with `-?` to list additional options.

See also: [TMCMG](//github.com/TOGoS/TMCMG/) (for generating entirely new terrain).
