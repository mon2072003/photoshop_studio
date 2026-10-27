# Photoshop Studio

Photoshop Studio is a Java Swing desktop image editor built as a Maven project for NetBeans. It provides a layer-based editing workflow with drawing tools, image overlays, text, shapes, filters, cropping, transformations, color controls, rulers, and history management.

## Features

### Canvas and files

- Create a new canvas
- Open common image files
- Save the composition as PNG
- Drag and drop images from File Explorer onto the canvas
- Place image overlays as independent layers
- Merge visible layers into the canvas
- Zoom from 10% to 800% with the mouse wheel, centered at the pointer, or use the footer zoom controls and Fit
- Pixel rulers around the canvas

### Tools

- Pointer tool for selecting and moving layers
- Brush and eraser tools with a style dropdown beside the Brush button: round, soft, square, marker, spray, chalk, calligraphy, and dotted styles
- Text tool with editable font family, style, size, color, border, and shadow
- Shape tool with an icon-only picker for rectangles, rounded rectangles, ellipses, lines, arrows, triangles, diamonds, pentagons, hexagons, octagons, stars, hearts, clouds, lightning, trapezoids, parallelograms, chevrons, plus/cross marks, speech bubbles, cylinders, arcs, and pies
- Selection tool
- Color Picker with a live magnifying lens and RGB readout
- Fill tool
- Gradient tool
- Crop tool with free, square, 4:3, 16:9, 3:2, circle, scrapbook, and perspective modes
- Interactive Rotate tool
- Online chatbot with Photoshop Studio editing guidance (no API key)

The chatbot is available from **Help → Chatbot**. It uses the anonymous DG AI API, so no API key or account is needed. An internet connection is required; anonymous requests are rate-limited and service availability may change. Chat messages are sent to the provider. It provides guidance and does not directly edit the canvas.

### Layers

Layers are displayed in the Layers panel with icons showing whether a layer is text, an image, or a shape. The Layer Properties section provides editable controls for:

- X and Y position
- Width and height
- Opacity
- Visibility
- Text-specific styling information

Layers can be reordered, hidden, deleted, duplicated through history, transformed, resized, rotated, flipped, edited, and isolated with the layer context menu.

### Effects and adjustments

The Effects menu contains grouped effects for easier navigation:

- Color and tone: grayscale, invert, sepia, cool tone, warm tone, colorize, duotone, exposure, highlights, shadows, and gamma
- Artistic and stylize: edge detect, posterize, threshold, emboss, vignette, pixelate, solarize, film grain, and glow
- Quality and detail: blur, sharpen, auto enhance, smart sharpen, reduce noise, and clarity
- Adjustable brightness, contrast, and saturation controls
- Portrait effects for face smoothing and warming, plus automatic hair and beard recoloring from the Effects menu

Effects can be applied to the full canvas or to the selected layer, and they support Undo and Redo.

To recolor hair or a beard, use the Select tool to make a tight rectangular selection around that region, then choose Effects → Portrait Effects → Hair or Beard → Color. Pick a color and the app recolors likely hair/beard pixels in the selected area while skipping likely skin tones and preserving brightness. The segmentation is heuristic, so a tight selection improves results.

## Requirements

- Java 17 or newer
- Apache Maven
- NetBeans is recommended for editing and running the project

## Installation (Windows)

Download the [Photoshop Studio Windows app bundle](downloads/PhotoshopStudio-1.0.0-windows-x64.zip) and extract the ZIP. Open the extracted `PhotoshopStudio` folder and run `PhotoshopStudio.exe`. The portable bundle includes its Java runtime, so a separate JDK installation is not needed. Keep the extracted app folder together when moving or sharing it.

## Build

From the project directory, run:

```bash
mvn clean package
```

The compiled JAR is created in the `target` directory.

## Run

Run the application with Maven:

```bash
mvn exec:java -Dexec.mainClass=com.mycompany.photoshopstudio.PhotoshopStudio
```

Or run the generated JAR using the Java runtime configured for your system.

## NetBeans

1. Open NetBeans.
2. Choose **File → Open Project**.
3. Select the `photoshopStudio` folder.
4. Clean and build the project.
5. Run the `PhotoshopStudio` main class.

## Project structure

```text
photoshopStudio/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    └── main/
        └── java/
            └── com/mycompany/photoshopstudio/
                └── PhotoshopStudio.java
```

## History and safety

The editor maintains an Undo/Redo history for image edits, layer transformations, effects, cropping, text styling, and other editing operations. When closing with unsaved edits, the application offers options to save the image, discard changes, or cancel closing.

## License

This project is provided for personal and educational use. Add an explicit license file if you plan to distribute or reuse the project publicly.

## Author

Mina Ezzat
