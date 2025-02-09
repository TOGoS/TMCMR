# TOGoS's Minecraft Map Renderer

Command-line Java application for generating top-down images of Minecraft worlds, one region at a time.

Supports translucent tiles and shading based on 'slope'.
Tile colors are configurable using a simple configuration file so can be updated to work with mods or newer versions of Minecraft.
By default colors are loaded from a built-in copy of these files.

Can automatically skip regeneration of tiles whose corresponding region has not been updated, based on file modification time.

![tile 0 0 8bit](https://github.com/user-attachments/assets/de895cf1-f939-4f65-b72b-2ac844030b3b)

## Basic Usage

```sh
java -jar TMCMR.jar region-directory -o output-directory
```

This will generate a PNG file for each region into the output directory along with tiles.html, which references all tiles in an HTML table so you can view the entire map.

If you run it again for the same regions and outputs, only regions that have been modified since their corresponding PNG files were generated (according to the filesystem timestamp) will be re-rendered.

Additional options:

```
Usage: TMCMR [options] -o <output-dir> <input-files>
  -h, -? ; print usage instructions and exit
  -f     ; force re-render even when images are newer than regions
  -debug ; be chatty
  -color-map <file>  ; load a custom color map from the specified file
  -biome-map <file>  ; load a custom biome color map from the specified file
  -create-tile-html  ; generate tiles.html in the output directory
  -create-image-tree ; generate a PicGrid-compatible image tree
  -create-big-image  ; merges all rendered images into a single file
  -min-height <y>    ; only draw blocks above this height
  -max-height <y>    ; only draw blocks below this height
  -region-limit-rect <x0> <y0> <x1> <y1> ; limit which regions are rendered
                     ; to those between the given region coordinates, e.g.
                     ; 0 0 2 2 to render the 4 regions southeast of the origin.
  -altitude-shading-factor <f>    ; how much altitude affects shading [36]
  -shading-reference-altitude <y> ; reference altitude for shading [64]
  -min-altitude-shading <x>       ; lowest altitude shading modifier [-20]
  -max-altitude-shading <x>       ; highest altitude shading modifier [20]
  -title <title>     ; title to include with maps
  -scales 1:<n>,...  ; list scales at which to render
  -threads <n>       ; maximum number of CPU threads to use for rendering
```

Input files may be 'region/' directories or individual '.mca' files.

tiles.html will always be generated if a single directory is given as input.

Compound image tree blobs will be written to `~/.ccouch/data/tmcmr/`.
Compound images can then be rendered with [PicGrid](https://github.com/TOGoS/PicGrid).

## Contributing

New versions of Minecraft introduce new blocks.
Updates to `block-colors.txt` and `biome-colors.txt` are welcome.

When making a PR, please try to minimize unnecessary changes.
e.g. avoid combining changes to functionality with ones that just reformat a source file.
I tend to reject those because they clutter the Git history and make it harder to see where a change originally came from.

## See Also

- [TMCMG](//github.com/TOGoS/TMCMG/) (for generating entirely new terrain)
