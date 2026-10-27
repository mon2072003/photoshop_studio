/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.mycompany.photoshopstudio;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.font.GlyphVector;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class PhotoshopStudio extends JFrame {
    static { installAppTypography(); }

    private final CanvasPanel canvas = new CanvasPanel();
    private JScrollPane canvasScroll;
    private final UndoManager undo = new UndoManager();
    private BufferedImage image;
    private BufferedImage beforeStroke;
    private Color brushColor = Color.BLACK;
    private int brushSize = 18;
    private boolean eraser;
    private Tool tool = Tool.BRUSH;
    private final JLabel status = new JLabel("Ready");
    private final List<MovableItem> movableItems = new ArrayList<>();
    private final DefaultListModel<MovableItem> layerModel = new DefaultListModel<>();
    private final JList<MovableItem> layerList = new JList<>(layerModel);
    private final JTextArea layerPropertiesText = new JTextArea();
    private JSpinner layerXSpinner, layerYSpinner, layerWidthSpinner, layerHeightSpinner;
    private JCheckBox layerVisibleCheck;
    private final JSlider opacitySlider = new JSlider(0, 100, 100);
    private JSlider brightnessSlider, contrastSlider, saturationSlider;
    private int appliedBrightnessValue, appliedContrastValue, appliedSaturationValue;
    private double cropAspect;
    private boolean circleCrop;
    private boolean scrapbookCrop;
    private boolean perspectiveCropMode;
    private JSlider redSlider, greenSlider, blueSlider;
    private JPanel colorPreviewPanel;
    private JButton colorButton;
    private JLabel pickerValueLabel;
    private JButton undoButton, redoButton;
    private JSlider brushSizeSlider;
    private JLabel zoomValueLabel;
    private JPanel zoomControlsPanel;
    private JComboBox<String> cropTypeSelector;
    private JMenu savedWorkspacesMenu;
    private String currentWorkspaceName;
    private final Preferences workspacePreferences = Preferences.userNodeForPackage(PhotoshopStudio.class);

    private enum Tool { MOVE, BRUSH, ERASER, TEXT, CROP, SHAPE, SELECT, EYEDROPPER, FILL, GRADIENT, ROTATE }
    private enum ShapeKind { RECTANGLE, ROUND_RECTANGLE, ELLIPSE, LINE, ARROW, TRIANGLE, DIAMOND, PENTAGON, HEXAGON, OCTAGON, STAR, SIX_POINT_STAR, HEART, CLOUD, LIGHTNING, TRAPEZOID, PARALLELOGRAM, CHEVRON, PLUS, CROSS, SPEECH_BUBBLE, CYLINDER, ARC, PIE }
    private enum BrushKind { ROUND, SOFT, SQUARE, MARKER, SPRAY, CHALK, CALLIGRAPHY, DOTTED }
    private ShapeKind shapeKind = ShapeKind.RECTANGLE;
    private BrushKind brushKind = BrushKind.ROUND;

    private static final class MovableItem {
        BufferedImage image; int x, y; String name; float opacity = 1f; boolean visible = true;
        String textContent; String fontFamily; int fontStyle; int fontSize; Color textColor;
        Color borderColor = new Color(0, 0, 0, 0); int borderWidth; boolean textShadow; Color shadowColor = new Color(0, 0, 0, 150); int shadowOffset = 4;
        MovableItem(BufferedImage image, int x, int y, String name) { this.image = image; this.x = x; this.y = y; this.name = name; }
        boolean contains(int px, int py) { return px >= x && py >= y && px < x + image.getWidth() && py < y + image.getHeight(); }
        @Override public String toString() { return (visible ? "● " : "○ ") + name; }
    }

    public PhotoshopStudio() {
        super("Photoshop Studio - Swing");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { attemptClose(); } });
        setSize(1100, 750);
        setLocationByPlatform(true);
        setIconImage(createAppIcon());
        newImage(900, 600);
        setJMenuBar(createMenuBar());
        add(createToolBar(), BorderLayout.NORTH);
        updateToolCursor();
        canvasScroll = new JScrollPane(canvas);
        canvasScroll.getViewport().setBackground(new Color(55, 55, 60));
        add(canvasScroll, BorderLayout.CENTER);
        add(createLayersPanel(), BorderLayout.EAST);
        JPanel footer = new JPanel(new BorderLayout(8, 0)); footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 208, 214)));
        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10)); footer.add(status, BorderLayout.CENTER);
        footer.add(zoomControlsPanel, BorderLayout.EAST); add(footer, BorderLayout.SOUTH);
        updateStatus("Ready");
    }

    private static void installAppTypography() {
        Font regular = new Font("Segoe UI", Font.PLAIN, 13);
        Font control = new Font("Segoe UI", Font.PLAIN, 12);
        Font menu = new Font("Segoe UI", Font.PLAIN, 13);
        Font message = new Font("Segoe UI", Font.PLAIN, 14);
        UIManager.put("Button.font", control);
        UIManager.put("ToggleButton.font", control);
        UIManager.put("Label.font", regular);
        UIManager.put("CheckBox.font", regular);
        UIManager.put("RadioButton.font", regular);
        UIManager.put("ComboBox.font", control);
        UIManager.put("Spinner.font", control);
        UIManager.put("Slider.font", control);
        UIManager.put("List.font", regular);
        UIManager.put("MenuBar.font", menu);
        UIManager.put("Menu.font", menu);
        UIManager.put("MenuItem.font", menu);
        UIManager.put("CheckBoxMenuItem.font", menu);
        UIManager.put("RadioButtonMenuItem.font", menu);
        UIManager.put("TextField.font", regular);
        UIManager.put("TextArea.font", regular);
        UIManager.put("TextPane.font", regular);
        UIManager.put("EditorPane.font", regular);
        UIManager.put("ToolTip.font", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("TitledBorder.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("OptionPane.messageFont", message);
        UIManager.put("OptionPane.buttonFont", control);
        UIManager.put("FileChooser.listFont", regular);
        UIManager.put("FileChooser.detailsView.font", regular);
    }

    private JPanel createLayersPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4)); panel.setBorder(BorderFactory.createTitledBorder("Layers")); panel.setPreferredSize(new Dimension(210, 0));
        layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layerList.setCellRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) { JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused); if (value instanceof MovableItem item) { label.setText((item.visible ? "" : "[Hidden] ") + item.name); label.setIcon(new EditorIcon(layerIconType(item))); label.setIconTextGap(6); } return label; } });
        layerList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) { canvas.selectedItem = layerList.getSelectedValue(); if (canvas.selectedItem != null) opacitySlider.setValue((int) (canvas.selectedItem.opacity * 100)); updateLayerProperties(); } });
        layerList.addMouseListener(new MouseAdapter() {
            private void showLayerMenu(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int index = layerList.locationToIndex(e.getPoint());
                if (index < 0) return;
                Rectangle bounds = layerList.getCellBounds(index, index);
                if (bounds == null || !bounds.contains(e.getPoint())) return;
                layerList.setSelectedIndex(index);
                MovableItem item = layerList.getModel().getElementAt(index);
                createLayerContextMenu(item).show(layerList, e.getX(), e.getY());
            }
            @Override public void mousePressed(MouseEvent e) { showLayerMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { showLayerMenu(e); }
        });
        panel.add(new JScrollPane(layerList), BorderLayout.CENTER);
        JPanel controls = new JPanel(new BorderLayout(2, 2));
        opacitySlider.setToolTipText("Selected layer opacity"); opacitySlider.addChangeListener(e -> { MovableItem item = layerList.getSelectedValue(); if (item != null) { item.opacity = opacitySlider.getValue() / 100f; updateLayerProperties(); canvas.repaint(); } });
        JButton visibility = new JButton("Show/Hide"); visibility.addActionListener(e -> { MovableItem item = layerList.getSelectedValue(); if (item != null) { item.visible = !item.visible; layerList.repaint(); canvas.repaint(); } });
        JButton editText = new JButton("Edit Text"); editText.addActionListener(e -> { MovableItem item = layerList.getSelectedValue(); if (item != null && item.textContent != null) editTextLayer(item); });
        JPanel bottom = new JPanel(new GridLayout(2, 1, 2, 2)); bottom.add(visibility); bottom.add(editText);
        controls.add(new JLabel("Opacity"), BorderLayout.NORTH); controls.add(opacitySlider, BorderLayout.CENTER); controls.add(bottom, BorderLayout.SOUTH);
        layerPropertiesText.setEditable(false); layerPropertiesText.setLineWrap(true); layerPropertiesText.setWrapStyleWord(true); layerPropertiesText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11)); layerPropertiesText.setBorder(BorderFactory.createTitledBorder("Layer Properties")); layerPropertiesText.setRows(7);
        layerXSpinner = new JSpinner(new SpinnerNumberModel(0, -10000, 10000, 1)); layerYSpinner = new JSpinner(new SpinnerNumberModel(0, -10000, 10000, 1)); layerWidthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1)); layerHeightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1)); layerVisibleCheck = new JCheckBox("Visible", true); JButton applyLayerProperties = new JButton("Apply Layer Changes"); applyLayerProperties.addActionListener(e -> applyLayerProperties());
        JPanel propertyEditor = new JPanel(new GridLayout(0, 2, 3, 3)); propertyEditor.add(new JLabel("X")); propertyEditor.add(layerXSpinner); propertyEditor.add(new JLabel("Y")); propertyEditor.add(layerYSpinner); propertyEditor.add(new JLabel("Width")); propertyEditor.add(layerWidthSpinner); propertyEditor.add(new JLabel("Height")); propertyEditor.add(layerHeightSpinner); propertyEditor.add(layerVisibleCheck); propertyEditor.add(applyLayerProperties);
        JPanel propertyArea = new JPanel(new BorderLayout(3, 3)); propertyArea.add(new JScrollPane(layerPropertiesText), BorderLayout.CENTER); propertyArea.add(propertyEditor, BorderLayout.SOUTH);
        JPanel lower = new JPanel(new BorderLayout(3, 3)); lower.add(controls, BorderLayout.NORTH); lower.add(propertyArea, BorderLayout.CENTER); lower.setPreferredSize(new Dimension(205, 310)); panel.add(lower, BorderLayout.SOUTH); updateLayerProperties();
        return panel;
    }

    private void updateLayerProperties() {
        MovableItem item = layerList.getSelectedValue(); if (item == null) { layerPropertiesText.setText("No layer selected"); return; }
        if (layerXSpinner != null) { layerXSpinner.setValue(item.x); layerYSpinner.setValue(item.y); layerWidthSpinner.setValue(item.image.getWidth()); layerHeightSpinner.setValue(item.image.getHeight()); layerVisibleCheck.setSelected(item.visible); }
        String type = item.textContent != null ? "Text" : item.name.toLowerCase().contains("shape") || item.name.matches("rectangle|ellipse|line|triangle|pentagon|hexagon|star|round_rectangle|diamond|arrow|cross") ? "Shape" : "Image";
        StringBuilder details = new StringBuilder(); details.append("Name: ").append(item.name).append("\nType: ").append(type).append("\nPosition: ").append(item.x).append(", ").append(item.y).append("\nSize: ").append(item.image.getWidth()).append(" × ").append(item.image.getHeight()).append(" px\nOpacity: ").append(Math.round(item.opacity * 100)).append("%\nVisible: ").append(item.visible);
        if (item.textContent != null) details.append("\nFont: ").append(item.fontFamily).append("\nFont size: ").append(item.fontSize).append("\nText color: ").append(item.textColor == null ? "None" : item.textColor.getRed() + ", " + item.textColor.getGreen() + ", " + item.textColor.getBlue()).append("\nBorder: ").append(item.borderWidth).append(" px\nShadow: ").append(item.textShadow ? "On" : "Off");
        layerPropertiesText.setText(details.toString()); layerPropertiesText.setCaretPosition(0);
    }

    private String layerIconType(MovableItem item) { if (item.textContent != null) return "text"; if (item.name != null && (item.name.matches("rectangle|ellipse|line|triangle|pentagon|hexagon|star|round_rectangle|diamond|arrow|cross") || item.name.toLowerCase().contains("shape"))) return "shape"; return "image"; }

    private void applyLayerProperties() {
        MovableItem item = layerList.getSelectedValue(); if (item == null) return;
        BufferedImage beforeImage = copy(item.image); int oldX = item.x, oldY = item.y; boolean oldVisible = item.visible; float oldOpacity = item.opacity; int newX = (Integer) layerXSpinner.getValue(), newY = (Integer) layerYSpinner.getValue(), newWidth = (Integer) layerWidthSpinner.getValue(), newHeight = (Integer) layerHeightSpinner.getValue();
        BufferedImage changedImage = beforeImage; if (newWidth != item.image.getWidth() || newHeight != item.image.getHeight()) { changedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB); Graphics2D g = changedImage.createGraphics(); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.drawImage(item.image, 0, 0, newWidth, newHeight, null); g.dispose(); }
        item.image = changedImage; item.x = newX; item.y = newY; item.visible = layerVisibleCheck.isSelected(); int newOpacity = opacitySlider.getValue(); item.opacity = newOpacity / 100f; BufferedImage afterImage = copy(item.image); boolean newVisible = item.visible; float afterOpacity = item.opacity;
        undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); item.image = copy(beforeImage); item.x = oldX; item.y = oldY; item.visible = oldVisible; item.opacity = oldOpacity; refreshLayers(); canvas.repaint(); } @Override public void redo() { super.redo(); item.image = copy(afterImage); item.x = newX; item.y = newY; item.visible = newVisible; item.opacity = afterOpacity; refreshLayers(); canvas.repaint(); } });
        updateLayerProperties(); canvas.revalidate(); canvas.repaint();
    }

    private void registerItem(MovableItem item) { movableItems.add(item); layerModel.addElement(item); layerList.setSelectedValue(item, true); }
    private void unregisterItem(MovableItem item) { movableItems.remove(item); layerModel.removeElement(item); }
    private void refreshLayers() { MovableItem selected = layerList.getSelectedValue(); layerModel.clear(); for (int i = movableItems.size() - 1; i >= 0; i--) layerModel.addElement(movableItems.get(i)); if (selected != null && movableItems.contains(selected)) layerList.setSelectedValue(selected, true); layerList.repaint(); updateLayerProperties(); }

    private JPopupMenu createLayerContextMenu(MovableItem item) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem select = new JMenuItem("Select"); select.addActionListener(e -> { layerList.setSelectedValue(item, true); canvas.selectedItem = item; updateLayerProperties(); canvas.repaint(); updateStatus("Selected " + item.name); });
        JMenuItem move = new JMenuItem("Move"); move.addActionListener(e -> { layerList.setSelectedValue(item, true); canvas.selectedItem = item; tool = Tool.MOVE; updateToolCursor(); updateStatus("Drag to move " + item.name); });
        JMenuItem visibility = new JMenuItem(item.visible ? "Hide Layer" : "Show Layer"); visibility.addActionListener(e -> { item.visible = !item.visible; layerList.repaint(); canvas.repaint(); updateLayerProperties(); });
        JMenuItem resize = new JMenuItem("Resize..."); resize.addActionListener(e -> resizeItem(item));
        JMenuItem rotate = new JMenuItem("Rotate Angle..."); rotate.addActionListener(e -> rotateItemFreely(item));
        JMenuItem flipH = new JMenuItem("Flip Horizontal"); flipH.addActionListener(e -> transformItem(item, "flipH"));
        JMenuItem flipV = new JMenuItem("Flip Vertical"); flipV.addActionListener(e -> transformItem(item, "flipV"));
        JMenuItem front = new JMenuItem("Bring to Front"); front.addActionListener(e -> { movableItems.remove(item); movableItems.add(item); refreshLayers(); canvas.repaint(); });
        JMenuItem back = new JMenuItem("Send to Back"); back.addActionListener(e -> { movableItems.remove(item); movableItems.add(0, item); refreshLayers(); canvas.repaint(); });
        JMenuItem delete = new JMenuItem("Delete Layer"); delete.addActionListener(e -> { unregisterItem(item); canvas.selectedItem = null; canvas.repaint(); updateLayerProperties(); });
        menu.add(select); menu.add(move);
        if (item.textContent != null) { JMenuItem edit = new JMenuItem("Edit Text..."); edit.addActionListener(e -> editTextLayer(item)); menu.add(edit); }
        menu.add(visibility); menu.addSeparator(); menu.add(resize); menu.add(rotate); menu.add(flipH); menu.add(flipV); menu.addSeparator(); menu.add(front); menu.add(back); menu.addSeparator(); menu.add(delete);
        return menu;
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New"); newItem.addActionListener(e -> newImage(900, 600));
        JMenuItem open = new JMenuItem("Open..."); open.addActionListener(e -> openImage());
        JMenuItem overlay = new JMenuItem("Place Image Overlay..."); overlay.addActionListener(e -> placeOverlay());
        JMenuItem save = new JMenuItem("Save As..."); save.addActionListener(e -> saveImage());
        JMenuItem exit = new JMenuItem("Exit"); exit.addActionListener(e -> attemptClose());
        file.add(newItem); file.add(open); file.add(overlay); file.add(save); file.addSeparator(); file.add(exit);
        JMenu edit = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo"); undoItem.addActionListener(e -> { if (undo.canUndo()) undo.undo(); syncControlValues(); canvas.repaint(); });
        JMenuItem redoItem = new JMenuItem("Redo"); redoItem.addActionListener(e -> { if (undo.canRedo()) undo.redo(); syncControlValues(); canvas.repaint(); });
        JMenuItem clear = new JMenuItem("Clear Canvas"); clear.addActionListener(e -> clearCanvas());
        edit.add(undoItem); edit.add(redoItem); edit.addSeparator(); edit.add(clear);
        JMenu imageMenu = new JMenu("Image");
        JMenuItem rotate = new JMenuItem("Rotate 90°"); rotate.addActionListener(e -> transformImage(true));
        JMenuItem flipH = new JMenuItem("Flip Horizontal"); flipH.addActionListener(e -> transformImage(false));
        JMenuItem info = new JMenuItem("Image Information"); info.addActionListener(e -> showImageInformation());
        JMenuItem merge = new JMenuItem("Merge / Flatten Images"); merge.addActionListener(e -> mergeImages());
        JMenuItem perspectiveCrop = new JMenuItem("Perspective Crop..."); perspectiveCrop.addActionListener(e -> perspectiveCrop());
        imageMenu.add(rotate); imageMenu.add(flipH); imageMenu.add(perspectiveCrop); imageMenu.add(merge); imageMenu.addSeparator(); imageMenu.add(info);
        JMenu effects = new JMenu("Effects"); JMenu colorEffects = new JMenu("Color & Tone"); JMenu artisticEffects = new JMenu("Artistic & Stylize"); JMenu qualityEffects = new JMenu("Quality & Detail"); JMenu adjustments = new JMenu("Adjustments"); JMenu portraitEffects = new JMenu("Portrait Effects");
        addEffect(colorEffects, "Grayscale", "grayscale"); addEffect(colorEffects, "Invert Colors", "invert"); addEffect(colorEffects, "Sepia", "sepia"); addEffect(colorEffects, "Cool Tone", "cool"); addEffect(colorEffects, "Warm Tone", "warm"); addEffect(colorEffects, "Colorize", "colorize"); addEffect(colorEffects, "Duotone", "duotone"); addEffect(colorEffects, "Exposure", "exposure"); addEffect(colorEffects, "Highlights", "highlights"); addEffect(colorEffects, "Shadows", "shadows"); addEffect(colorEffects, "Gamma", "gamma");
        addEffect(artisticEffects, "Edge Detect", "edge"); addEffect(artisticEffects, "Posterize", "posterize"); addEffect(artisticEffects, "Threshold", "threshold"); addEffect(artisticEffects, "Emboss", "emboss"); addEffect(artisticEffects, "Vignette", "vignette"); addEffect(artisticEffects, "Pixelate", "pixelate"); addEffect(artisticEffects, "Solarize", "solarize"); addEffect(artisticEffects, "Film Grain", "filmGrain"); addEffect(artisticEffects, "Glow", "glow");
        addEffect(qualityEffects, "Blur", "blur"); addEffect(qualityEffects, "Sharpen", "sharpen"); addEffect(qualityEffects, "Auto Enhance", "autoEnhance"); addEffect(qualityEffects, "Smart Sharpen", "smartSharpen"); addEffect(qualityEffects, "Reduce Noise", "reduceNoise"); addEffect(qualityEffects, "Clarity", "clarity");
        addAdjustment(adjustments, "Brightness...", "brightness"); addAdjustment(adjustments, "Contrast...", "contrastAdjust"); addAdjustment(adjustments, "Saturation...", "saturationAdjust");
        JMenu faceEffects = new JMenu("Face"); addPortraitEffect(faceEffects, "Smooth Skin", "smoothSkin"); addPortraitEffect(faceEffects, "Warm Skin Tone", "warmSkin");
        JMenu hairEffects = new JMenu("Hair"); addPortraitColorEffect(hairEffects, "Hair Color...", "hairColor");
        JMenu beardEffects = new JMenu("Beard"); addPortraitColorEffect(beardEffects, "Beard Color...", "beardColor");
        portraitEffects.add(faceEffects); portraitEffects.add(hairEffects); portraitEffects.add(beardEffects);
        effects.add(colorEffects); effects.add(artisticEffects); effects.add(qualityEffects); effects.add(portraitEffects); effects.add(adjustments);
        JMenu view = new JMenu("View");
        addZoomItem(view, "50%", 0.5); addZoomItem(view, "100%", 1.0); addZoomItem(view, "150%", 1.5); addZoomItem(view, "200%", 2.0);
        view.addSeparator(); JMenuItem reset = new JMenuItem("Reset View"); reset.addActionListener(e -> { canvas.setZoom(1.0); updateStatus("View reset"); }); view.add(reset);
        JMenu help = new JMenu("Help"); JMenuItem aiChat = new JMenuItem("Chatbot"); aiChat.addActionListener(e -> showAiChat()); JMenuItem about = new JMenuItem("About Photoshop Studio"); about.addActionListener(e -> showAboutDialog()); help.add(aiChat); help.add(about);
        bar.add(file); bar.add(edit); bar.add(imageMenu); bar.add(effects); bar.add(view); bar.add(help); return bar;
    }

    private void styleMenuBar(JMenuBar bar) {
        bar.setBackground(new Color(58, 58, 64));
        for (MenuElement element : bar.getSubElements()) styleMenuElement(element);
    }

    private void styleMenuElement(MenuElement element) {
        if (element.getComponent() instanceof JComponent component) { component.setForeground(Color.WHITE); component.setBackground(new Color(76, 76, 84)); }
        for (MenuElement child : element.getSubElements()) styleMenuElement(child);
    }


    private void addZoomItem(JMenu menu, String label, double zoom) { JMenuItem item = new JMenuItem(label); item.addActionListener(e -> { canvas.setZoom(zoom); updateStatus("Zoom: " + label); }); menu.add(item); }

    private void addEffect(JMenu menu, String label, String effect) {
        JMenuItem item = new JMenuItem(label); item.addActionListener(e -> applyEffect(effect)); menu.add(item);
    }

    private void addPortraitEffect(JMenu menu, String label, String effect) { JMenuItem item = new JMenuItem(label); item.addActionListener(e -> applyPortraitEffect(effect, null)); menu.add(item); }
    private void addPortraitColorEffect(JMenu menu,String label,String effect) { JMenuItem item=new JMenuItem(label); item.addActionListener(e->{if(!requirePortraitSelection())return;Color chosen=JColorChooser.showDialog(this,label,brushColor);if(chosen!=null)applyPortraitEffect(effect,chosen);});menu.add(item); }
    private void addAdjustment(JMenu menu, String label, String type) { JMenuItem item = new JMenuItem(label); item.addActionListener(e -> adjustColor(type)); menu.add(item); }

    private void showAiChat() {
        JDialog dialog=new JDialog(this,"Chatbot",false);dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);dialog.setIconImage(createAppIcon());
        JPanel root=new JPanel(new BorderLayout(0,0));root.setBackground(Color.WHITE);
        JPanel header=new JPanel(new BorderLayout(12,0));header.setBackground(new Color(44,48,56));header.setBorder(BorderFactory.createEmptyBorder(13,16,13,14));
        JLabel mark=new JLabel("Ps");mark.setOpaque(true);mark.setHorizontalAlignment(SwingConstants.CENTER);mark.setForeground(Color.WHITE);mark.setBackground(new Color(54,112,194));mark.setFont(mark.getFont().deriveFont(Font.BOLD,18f));mark.setPreferredSize(new Dimension(42,42));
        JPanel titleBlock=new JPanel();titleBlock.setOpaque(false);titleBlock.setLayout(new BoxLayout(titleBlock,BoxLayout.Y_AXIS));JLabel title=new JLabel("Photoshop Studio Chatbot");title.setForeground(Color.WHITE);title.setFont(title.getFont().deriveFont(Font.BOLD,16f));JLabel subtitle=new JLabel("Ask about tools, layers, and editing workflows");subtitle.setForeground(new Color(200,204,212));titleBlock.add(title);titleBlock.add(Box.createVerticalStrut(3));titleBlock.add(subtitle);
        JButton clear=new JButton("Clear chat");clear.setToolTipText("Start a fresh conversation");clear.setFocusable(false);JPanel heading=new JPanel(new BorderLayout(10,0));heading.setOpaque(false);heading.add(mark,BorderLayout.WEST);heading.add(titleBlock,BorderLayout.CENTER);header.add(heading,BorderLayout.CENTER);header.add(clear,BorderLayout.EAST);
        JPanel transcript=new JPanel();transcript.setLayout(new BoxLayout(transcript,BoxLayout.Y_AXIS));transcript.setBackground(new Color(247,248,250));transcript.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JScrollPane transcriptScroll=new JScrollPane(transcript);transcriptScroll.setBorder(BorderFactory.createEmptyBorder());transcriptScroll.getViewport().setBackground(new Color(247,248,250));transcriptScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JTextArea message=new JTextArea(3,30){@Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);if(getText().isEmpty()&&!hasFocus()){graphics.setColor(new Color(145,150,160));graphics.drawString("Ask about tools, effects, or editing…",getInsets().left+2,getInsets().top+getFontMetrics(getFont()).getAscent());}}};message.setLineWrap(true);message.setWrapStyleWord(true);message.setFont(new Font("Segoe UI",Font.PLAIN,14));message.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));message.setToolTipText("Type a message; Ctrl+Enter sends it");
        JPanel inputFrame=new JPanel(new BorderLayout());inputFrame.setBackground(Color.WHITE);inputFrame.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(190,197,208),1,true),BorderFactory.createEmptyBorder(1,1,1,1)));JScrollPane messageScroll=new JScrollPane(message);messageScroll.setBorder(BorderFactory.createEmptyBorder());messageScroll.setOpaque(false);messageScroll.getViewport().setOpaque(false);messageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);inputFrame.add(messageScroll,BorderLayout.CENTER);
        JButton send=new ChatSendButton();send.setToolTipText("Send message (Ctrl+Enter)");send.setPreferredSize(new Dimension(98,48));
        message.addFocusListener(new FocusAdapter(){private void update(){Color line=message.hasFocus()?new Color(58,125,210):new Color(190,197,208);inputFrame.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(line,message.hasFocus()?2:1,true),BorderFactory.createEmptyBorder(1,1,1,1)));} @Override public void focusGained(FocusEvent e){update();}@Override public void focusLost(FocusEvent e){update();}});
        JLabel state=new JLabel("●  Online chatbot · no API key");state.setForeground(new Color(65,125,85));state.setBorder(BorderFactory.createEmptyBorder(0,2,0,0));
        JLabel privacy=new JLabel("Messages are sent to the online provider. Free anonymous use is rate-limited.");privacy.setForeground(new Color(105,110,120));privacy.setFont(privacy.getFont().deriveFont(11f));
        JPanel inputRow=new JPanel(new BorderLayout(10,0));inputRow.setOpaque(false);inputRow.add(inputFrame,BorderLayout.CENTER);inputRow.add(send,BorderLayout.EAST);
        JPanel composer=new JPanel(new BorderLayout(0,7));composer.setBackground(Color.WHITE);composer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,new Color(225,228,233)),BorderFactory.createEmptyBorder(10,14,11,14)));composer.add(inputRow,BorderLayout.CENTER);JPanel foot=new JPanel(new BorderLayout(0,4));foot.setOpaque(false);foot.add(state,BorderLayout.NORTH);foot.add(privacy,BorderLayout.SOUTH);composer.add(foot,BorderLayout.SOUTH);
        root.add(header,BorderLayout.NORTH);root.add(transcriptScroll,BorderLayout.CENTER);root.add(composer,BorderLayout.SOUTH);dialog.setContentPane(root);dialog.setMinimumSize(new Dimension(560,440));dialog.setSize(720,560);dialog.setLocationRelativeTo(this);
        List<String[]> history=new ArrayList<>();appendChatLine(transcript,transcriptScroll,"Chatbot","Hi! I can help with Photoshop Studio tools and image-editing workflows. What would you like to do?");
        Runnable sendMessage=()->{String prompt=message.getText().trim();if(prompt.isEmpty()||!send.isEnabled())return;appendChatLine(transcript,transcriptScroll,"You",prompt);history.add(new String[]{"user",prompt});message.setText("");send.setEnabled(false);clear.setEnabled(false);state.setText("●  Waiting for the online chatbot…");state.setForeground(new Color(173,117,35));List<String[]> snapshot=new ArrayList<>(history);
            SwingWorker<String,Void> worker=new SwingWorker<>(){@Override protected String doInBackground()throws Exception{return requestFreeOnlineReply(snapshot);}@Override protected void done(){send.setEnabled(true);clear.setEnabled(true);try{String reply=get();history.add(new String[]{"assistant",reply});appendChatLine(transcript,transcriptScroll,"Chatbot",reply);state.setText("●  Online chatbot · no API key");state.setForeground(new Color(65,125,85));}catch(Exception ex){Throwable cause=ex.getCause()==null?ex:ex.getCause();state.setText("●  Connection failed · try again");state.setForeground(new Color(175,65,65));appendChatLine(transcript,transcriptScroll,"Connection error",cause.getMessage()==null?cause.toString():cause.getMessage());}message.requestFocusInWindow();}};worker.execute();};
        send.addActionListener(e->sendMessage.run());message.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.CTRL_DOWN_MASK),"send-chat");message.getActionMap().put("send-chat",new AbstractAction(){@Override public void actionPerformed(ActionEvent e){sendMessage.run();}});
        clear.addActionListener(e->{history.clear();transcript.removeAll();appendChatLine(transcript,transcriptScroll,"Chatbot","New conversation started. How can I help?");state.setText("●  Online chatbot · no API key");state.setForeground(new Color(65,125,85));});
        dialog.setVisible(true);message.requestFocusInWindow();
    }

    private void appendChatLine(JPanel transcript,JScrollPane scroll,String speaker,String text){boolean user=speaker.equals("You"),error=speaker.equals("Connection error");Color fill=user?new Color(224,237,255):error?new Color(255,235,234):Color.WHITE;Color accent=user?new Color(43,99,174):error?new Color(175,65,65):new Color(47,119,79);int maxWidth=Math.min(520,Math.max(260,scroll.getViewport().getWidth()-100));JPanel bubble=new JPanel(new BorderLayout(0,5)){@Override protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(fill);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);g.setColor(new Color(224,228,234));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);g.dispose();}};bubble.setOpaque(false);bubble.setBorder(BorderFactory.createEmptyBorder(10,13,10,13));JLabel who=new JLabel(speaker);who.setForeground(accent);who.setFont(who.getFont().deriveFont(Font.BOLD,12f));JTextArea content=new JTextArea(text);content.setEditable(false);content.setOpaque(false);content.setLineWrap(true);content.setWrapStyleWord(true);content.setFont(new Font("Segoe UI",Font.PLAIN,14));content.setForeground(new Color(40,43,49));content.setBorder(null);content.setSize(new Dimension(maxWidth-28,Short.MAX_VALUE));Dimension textSize=content.getPreferredSize();content.setPreferredSize(new Dimension(maxWidth-28,Math.max(22,textSize.height)));bubble.add(who,BorderLayout.NORTH);bubble.add(content,BorderLayout.CENTER);bubble.setPreferredSize(new Dimension(maxWidth,Math.max(58,textSize.height+42)));bubble.setMaximumSize(new Dimension(maxWidth,Math.max(58,textSize.height+42)));JPanel row=new JPanel(new BorderLayout());row.setOpaque(false);row.setBorder(BorderFactory.createEmptyBorder(3,5,5,5));row.setAlignmentX(Component.LEFT_ALIGNMENT);if(user)row.add(bubble,BorderLayout.EAST);else row.add(bubble,BorderLayout.WEST);row.setPreferredSize(new Dimension(Math.max(500,scroll.getViewport().getWidth()),Math.max(64,textSize.height+52)));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,Math.max(64,textSize.height+52)));transcript.add(row);transcript.revalidate();transcript.repaint();SwingUtilities.invokeLater(()->scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));}

    private static final class ChatSendButton extends JButton {
        ChatSendButton(){super("Send",new ChatSendIcon());setForeground(Color.WHITE);setFont(getFont().deriveFont(Font.BOLD,13f));setHorizontalTextPosition(SwingConstants.LEFT);setIconTextGap(8);setFocusPainted(false);setBorder(BorderFactory.createEmptyBorder(8,13,8,13));setContentAreaFilled(false);setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);ButtonModel m=getModel();Color base=!isEnabled()?new Color(155,165,180):m.isPressed()?new Color(35,86,155):m.isRollover()?new Color(62,132,220):new Color(48,112,198);g.setColor(base);g.fillRoundRect(0,0,getWidth(),getHeight(),14,14);g.dispose();super.paintComponent(graphics);}
    }

    private static final class ChatSendIcon implements Icon {
        @Override public int getIconWidth(){return 19;}@Override public int getIconHeight(){return 19;}
        @Override public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.setColor(Color.WHITE);g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));Path2D p=new Path2D.Double();p.moveTo(x+2,y+8);p.lineTo(x+17,y+2);p.lineTo(x+12,y+17);p.lineTo(x+8,y+11);p.closePath();g.draw(p);g.drawLine(x+8,y+11,x+17,y+2);g.dispose();}
    }

    private String requestFreeOnlineReply(List<String[]> history)throws Exception{
        StringBuilder messages=new StringBuilder("[\n{\"role\":\"system\",\"content\":").append(jsonEscape("You are the Photoshop Studio in-app assistant. Give helpful, concise answers about image editing and this Java Swing app. Do not claim to directly modify the user's image.")).append('}');
        int start=Math.max(0,history.size()-20);for(int i=start;i<history.size();i++){String[] entry=history.get(i);messages.append(",\n{\"role\":").append(jsonEscape(entry[0])).append(",\"content\":").append(jsonEscape(entry[1])).append('}');}messages.append("\n]");
        String body="{\"model\":\"gpt-oss\",\"messages\":"+messages+"}";
        HttpRequest request=HttpRequest.newBuilder(URI.create("https://dg-ai.scriptsnsenses.workers.dev/v1/chat")).timeout(Duration.ofSeconds(90)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build();
        HttpResponse<String> response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build().send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(response.statusCode()<200||response.statusCode()>=300){String detail=extractJsonString(response.body(),"error");throw new IOException("Online chatbot error ("+response.statusCode()+"): "+(detail==null?response.body():detail));}
        String reply=extractJsonString(response.body(),"reply");if(reply==null||reply.isBlank())throw new IOException("The online chatbot returned an empty response.");return reply;
    }

    private String jsonEscape(String value){StringBuilder out=new StringBuilder("\"");for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<0x20)out.append(String.format("\\u%04x",(int)c));else out.append(c);}}}return out.append('"').toString();}

    private String extractJsonString(String json,String key){int keyAt=json.indexOf("\""+key+"\"");if(keyAt<0)return null;int colon=json.indexOf(':',keyAt+key.length()+2);if(colon<0)return null;int i=colon+1;while(i<json.length()&&Character.isWhitespace(json.charAt(i)))i++;if(i>=json.length()||json.charAt(i)!='\"')return null;i++;StringBuilder out=new StringBuilder();while(i<json.length()){char c=json.charAt(i++);if(c=='\"')return out.toString();if(c!='\\'){out.append(c);continue;}if(i>=json.length())break;char esc=json.charAt(i++);switch(esc){case '"','\\','/'->out.append(esc);case 'b'->out.append('\b');case 'f'->out.append('\f');case 'n'->out.append('\n');case 'r'->out.append('\r');case 't'->out.append('\t');case 'u'->{if(i+4<=json.length()){try{out.append((char)Integer.parseInt(json.substring(i,i+4),16));i+=4;}catch(NumberFormatException ignored){}}}default->out.append(esc);}}return null;}

    private void showAboutDialog() {
        JDialog dialog = new JDialog(this, "About Photoshop Studio", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setIconImage(createAppIcon());
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));

        JPanel heading = new JPanel(new BorderLayout(14, 0));
        JLabel icon = new JLabel(new ImageIcon(createAppIcon().getScaledInstance(64, 64, Image.SCALE_SMOOTH)));
        heading.add(icon, BorderLayout.WEST);
        JPanel titleBlock = new JPanel(); titleBlock.setOpaque(false); titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Photoshop Studio"); title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("A desktop image editor built with Java Swing"); subtitle.setForeground(new Color(95, 100, 110));
        titleBlock.add(Box.createVerticalGlue()); titleBlock.add(title); titleBlock.add(Box.createVerticalStrut(5)); titleBlock.add(subtitle); titleBlock.add(Box.createVerticalGlue());
        heading.add(titleBlock, BorderLayout.CENTER);
        content.add(heading, BorderLayout.NORTH);

        JTextArea details = new JTextArea(
                "ABOUT\n" +
                "Photoshop Studio is a hands-on desktop editor for arranging images, drawing, adding text and shapes, and applying visual adjustments. The interface is designed around a canvas, a tool bar, and an editable layer list.\n\n" +
                "EDITING TOOLS\n" +
                "• Brush and eraser for painting on the canvas or on an image layer\n" +
                "• Pointer tool for moving image and text layers; free rotation and resizing\n" +
                "• Text layers with font, color, outline, and shadow controls\n" +
                "• Rectangles, ellipses, lines, polygons, stars, arrows, and other shapes\n" +
                "• Color picker with a magnified preview, fill, and gradient tools\n" +
                "• Drag-and-drop image placement and support for multiple layers\n\n" +
                "CROPPING AND IMAGE EFFECTS\n" +
                "Use freeform, fixed-ratio, circle, scrapbook, or perspective crop modes. The Effects menu includes color and tone changes, artistic treatments, blur and detail tools, and brightness, contrast, and saturation adjustments. Effects can be applied to the canvas or the selected image layer.\n\n" +
                "LAYERS AND WORKFLOW\n" +
                "The Layers panel lets you select, reorder, show or hide, resize, transform, and adjust the opacity of layers. Right-click an object or layer for context actions. Undo and redo are available while editing. Zoom with the mouse wheel over the canvas; the pointer position anchors the zoom. Use Fit to bring the full image into view.\n\n" +
                "FILES\n" +
                "Open common image formats supported by Java ImageIO and export the finished composite as a PNG image. The Windows app bundle includes its own Java runtime, so a separate JDK installation is not required.\n\n" +
                "PROJECT DETAILS\n" +
                "Author: Mina Ezzat\n" +
                "Version: 1.0\n" +
                "Technology: Java 17, Swing, and Java ImageIO\n" +
                "This is an independent image-editing project. It is not affiliated with Adobe Inc."
        );
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true); details.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        details.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14)); details.setBackground(new Color(250, 250, 252));
        JScrollPane scroll = new JScrollPane(details); scroll.setBorder(BorderFactory.createLineBorder(new Color(215, 218, 224))); scroll.getVerticalScrollBar().setUnitIncrement(18);
        content.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        JLabel author = new JLabel("Mina Ezzat  •  Photoshop Studio 1.0"); author.setForeground(new Color(100, 105, 115));
        JButton close = new JButton("Close"); close.addActionListener(e -> dialog.dispose());
        footer.add(author, BorderLayout.WEST); footer.add(close, BorderLayout.EAST); content.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(content); dialog.setSize(650, 650); dialog.setMinimumSize(new Dimension(540, 500)); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    private void applyAdjustmentValue(String type, int amount) {
        if (amount == 0) return;
        MovableItem selected = layerList.getSelectedValue(); if (selected == null) selected = canvas.selectedItem; BufferedImage target = selected == null ? image : selected.image, before = copy(target);
        int beforeSliderValue = getAppliedAdjustmentValue(type);
        for (int y = 0; y < target.getHeight(); y++) for (int x = 0; x < target.getWidth(); x++) { int argb = target.getRGB(x, y), a = argb & 0xff000000, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255; if (type.equals("brightness")) { r = clamp(r + amount); g = clamp(g + amount); b = clamp(b + amount); } else if (type.equals("contrastAdjust")) { double f = 1 + amount / 100.0; r = clamp((int) ((r - 128) * f + 128)); g = clamp((int) ((g - 128) * f + 128)); b = clamp((int) ((b - 128) * f + 128)); } else { int avg = (r + g + b) / 3; double f = 1 + amount / 100.0; r = clamp((int) (avg + (r - avg) * f)); g = clamp((int) (avg + (g - avg) * f)); b = clamp((int) (avg + (b - avg) * f)); } target.setRGB(x, y, a | (r << 16) | (g << 8) | b); }
        final MovableItem adjustmentItem = selected;
        BufferedImage after = copy(target); setAppliedAdjustmentValue(type, amount);
        undo.addEdit(new AbstractUndoableEdit() {
            @Override public void undo() { super.undo(); if (adjustmentItem == null) image = copy(before); else adjustmentItem.image = copy(before); setAppliedAdjustmentValue(type, beforeSliderValue); canvas.revalidate(); canvas.repaint(); }
            @Override public void redo() { super.redo(); if (adjustmentItem == null) image = copy(after); else adjustmentItem.image = copy(after); setAppliedAdjustmentValue(type, amount); canvas.revalidate(); canvas.repaint(); }
        });
        canvas.repaint();
    }

    private int getAppliedAdjustmentValue(String type) {
        if (type.equals("brightness")) return appliedBrightnessValue;
        if (type.equals("contrastAdjust")) return appliedContrastValue;
        return appliedSaturationValue;
    }

    private void setAppliedAdjustmentValue(String type, int value) {
        if (type.equals("brightness")) { appliedBrightnessValue = value; if (brightnessSlider != null) brightnessSlider.setValue(value); }
        else if (type.equals("contrastAdjust")) { appliedContrastValue = value; if (contrastSlider != null) contrastSlider.setValue(value); }
        else { appliedSaturationValue = value; if (saturationSlider != null) saturationSlider.setValue(value); }
    }

    private void adjustColor(String type) {
        JSlider slider = new JSlider(-100, 100, 0); slider.setMajorTickSpacing(50); slider.setPaintTicks(true); slider.setPaintLabels(true);
        JPanel panel = new JPanel(new BorderLayout(6, 6)); panel.add(new JLabel(type.equals("brightness") ? "Brightness" : type.equals("contrastAdjust") ? "Contrast" : "Saturation"), BorderLayout.NORTH); panel.add(slider, BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(this, panel, "Adjust " + type, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        MovableItem selected = layerList.getSelectedValue(); if (selected == null) selected = canvas.selectedItem; BufferedImage target = selected == null ? image : selected.image, before = copy(target); int amount = slider.getValue();
        for (int y = 0; y < target.getHeight(); y++) for (int x = 0; x < target.getWidth(); x++) { int argb = target.getRGB(x, y), a = argb & 0xff000000, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255; if (type.equals("brightness")) { r = clamp(r + amount); g = clamp(g + amount); b = clamp(b + amount); } else if (type.equals("contrastAdjust")) { double f = 1 + amount / 100.0; r = clamp((int) ((r - 128) * f + 128)); g = clamp((int) ((g - 128) * f + 128)); b = clamp((int) ((b - 128) * f + 128)); } else { int avg = (r + g + b) / 3; double f = 1 + amount / 100.0; r = clamp((int) (avg + (r - avg) * f)); g = clamp((int) (avg + (g - avg) * f)); b = clamp((int) (avg + (b - avg) * f)); } target.setRGB(x, y, a | (r << 16) | (g << 8) | b); }
        if (selected == null) addImageEdit(before, copy(image)); else addItemImageEdit(selected, before, copy(selected.image)); canvas.repaint();
    }

    private JToolBar createToolBar() {
        JToolBar bar = new JToolBar();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setFloatable(false); bar.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        undoButton = new JButton(new EditorIcon("undo")); undoButton.setToolTipText("Undo (Ctrl+Z)");
        redoButton = new JButton(new EditorIcon("redo")); redoButton.setToolTipText("Redo (Ctrl+Y)");
        undoButton.setEnabled(false); redoButton.setEnabled(false);
        undoButton.addActionListener(e -> { if (undo.canUndo()) undo.undo(); syncControlValues(); canvas.repaint(); updateStatus("Undo"); });
        redoButton.addActionListener(e -> { if (undo.canRedo()) undo.redo(); syncControlValues(); canvas.repaint(); updateStatus("Redo"); });
        JToggleButton brush = new JToggleButton("Brush", new EditorIcon("brush"), true);
        JToggleButton move = new JToggleButton("Pointer", new EditorIcon("move"));
        JToggleButton erase = new JToggleButton("Eraser", new EditorIcon("eraser"));
        JToggleButton text = new JToggleButton("Text", new EditorIcon("text"));
        JToggleButton crop = new JToggleButton("Crop", new EditorIcon("crop"));
        JComboBox<String> cropType = new JComboBox<>(new String[]{"Free Crop", "Square 1:1", "Photo 4:3", "Widescreen 16:9", "Photo 3:2", "Circle Crop", "Scrapbook Crop", "Perspective Crop"}); cropTypeSelector = cropType; cropType.setToolTipText("Crop shape and aspect ratio"); cropType.setPreferredSize(new Dimension(165, 28));
        JToggleButton shape = new JToggleButton("Shape", new EditorIcon("shape"));
        JToggleButton rotate = new JToggleButton("Rotate", new EditorIcon("rotate"));
        JToggleButton select = new JToggleButton("Select", new EditorIcon("select")); JToggleButton eyedropper = new JToggleButton("Picker", new EditorIcon("picker")); JToggleButton fill = new JToggleButton("Fill", new EditorIcon("fill")); JToggleButton gradient = new JToggleButton("Gradient", new EditorIcon("gradient"));
        brush.setToolTipText("Draw with the selected color"); erase.setToolTipText("Erase pixels"); text.setToolTipText("Click the canvas to add text"); crop.setToolTipText("Drag a region, then crop"); select.setToolTipText("Drag a rectangular selection"); eyedropper.setToolTipText("Pick a color from the canvas"); fill.setToolTipText("Fill a connected area"); gradient.setToolTipText("Drag a linear gradient");
        ButtonGroup group = new ButtonGroup(); group.add(brush); group.add(move); group.add(erase); group.add(text); group.add(crop); group.add(shape); group.add(rotate); group.add(select); group.add(eyedropper); group.add(fill); group.add(gradient);
        brush.setToolTipText("Paint on the image"); move.setToolTipText("Select and move text or overlay images");
        brush.addActionListener(e -> { eraser = false; tool = Tool.BRUSH; updateToolCursor(); });
        move.addActionListener(e -> { eraser = false; tool = Tool.MOVE; updateToolCursor(); });
        erase.addActionListener(e -> { eraser = true; tool = Tool.ERASER; updateToolCursor(); });
        text.addActionListener(e -> { eraser = false; tool = Tool.TEXT; updateToolCursor(); });
        crop.addActionListener(e -> { eraser = false; tool = Tool.CROP; updateToolCursor(); });
        cropType.addActionListener(e -> { int selected = cropType.getSelectedIndex(); circleCrop = selected == 5; scrapbookCrop = selected == 6; perspectiveCropMode = selected == 7; cropAspect = switch (selected) { case 1, 5 -> 1.0; case 2 -> 4.0 / 3.0; case 3 -> 16.0 / 9.0; case 4 -> 3.0 / 2.0; default -> 0.0; }; if (perspectiveCropMode) canvas.clearPerspectivePoints(); });
        shape.addActionListener(e -> { eraser = false; tool = Tool.SHAPE; updateToolCursor(); });
        rotate.setToolTipText("Drag around a layer to rotate it"); rotate.addActionListener(e -> { eraser = false; tool = Tool.ROTATE; updateToolCursor(); });
        select.addActionListener(e -> { eraser = false; tool = Tool.SELECT; updateToolCursor(); }); eyedropper.addActionListener(e -> { eraser = false; tool = Tool.EYEDROPPER; updateToolCursor(); }); fill.addActionListener(e -> { eraser = false; tool = Tool.FILL; updateToolCursor(); }); gradient.addActionListener(e -> { eraser = false; tool = Tool.GRADIENT; updateToolCursor(); });
        JButton color = new JButton("Color", new EditorIcon("color")); colorButton = color; color.setToolTipText("Choose brush/text color");
        JSlider size = new JSlider(2, 80, brushSize); brushSizeSlider = size; size.setToolTipText("Brush and text size"); size.setPreferredSize(new Dimension(140, 28));
        size.addChangeListener(e -> brushSize = size.getValue());
        JComboBox<BrushKind> brushType = new JComboBox<>(BrushKind.values()); brushType.setSelectedItem(brushKind); brushType.setToolTipText("Brush style"); brushType.setRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);if(value instanceof BrushKind kind){label.setText(brushKindLabel(kind));label.setIcon(new BrushPreviewIcon(kind));}return label;}}); brushType.setPreferredSize(new Dimension(145,27)); brushType.addActionListener(e->{if(brushType.getSelectedItem() instanceof BrushKind kind){brushKind=kind;brushType.setToolTipText("Brush style: "+brushKindLabel(kind));}});
        brightnessSlider = new JSlider(-100, 100, appliedBrightnessValue); contrastSlider = new JSlider(-100, 100, appliedContrastValue); saturationSlider = new JSlider(-100, 100, appliedSaturationValue);
        JSlider brightness = brightnessSlider, contrast = contrastSlider, saturation = saturationSlider;
        brightness.setToolTipText("Brightness adjustment"); contrast.setToolTipText("Contrast adjustment"); saturation.setToolTipText("Saturation adjustment");
        JPanel adjustments = new JPanel(new GridLayout(3, 2, 1, 0)); adjustments.setOpaque(false); adjustments.add(controlIconLabel("brightness", "Brightness")); adjustments.add(brightness); adjustments.add(controlIconLabel("contrast", "Contrast")); adjustments.add(contrast); adjustments.add(controlIconLabel("saturation", "Saturation")); adjustments.add(saturation); adjustments.setPreferredSize(new Dimension(95, 36));
        brightness.addMouseListener(new MouseAdapter() { @Override public void mouseReleased(MouseEvent e) { applyAdjustmentValue("brightness", brightness.getValue()); } });
        contrast.addMouseListener(new MouseAdapter() { @Override public void mouseReleased(MouseEvent e) { applyAdjustmentValue("contrastAdjust", contrast.getValue()); } });
        saturation.addMouseListener(new MouseAdapter() { @Override public void mouseReleased(MouseEvent e) { applyAdjustmentValue("saturationAdjust", saturation.getValue()); } });
        JButton applyAdjustments = new JButton("Apply"); applyAdjustments.setToolTipText("Apply brightness, contrast, and saturation"); applyAdjustments.addActionListener(e -> { applyAdjustmentValue("brightness", brightness.getValue()); applyAdjustmentValue("contrastAdjust", contrast.getValue()); applyAdjustmentValue("saturationAdjust", saturation.getValue()); });
        JComboBox<ShapeKind> shapeType = new JComboBox<>(ShapeKind.values()); shapeType.setToolTipText("Shape: Rectangle"); shapeType.setRenderer(new ShapeListRenderer()); shapeType.setMaximumRowCount(10);
        shapeType.getAccessibleContext().setAccessibleName("Shape picker"); shapeType.getAccessibleContext().setAccessibleDescription("Choose a shape by its icon");
        shapeType.addActionListener(e -> { shapeKind = (ShapeKind) shapeType.getSelectedItem(); if(shapeKind!=null) shapeType.setToolTipText("Shape: "+shapeKindLabel(shapeKind)); });
        redSlider = colorSlider(brushColor.getRed()); greenSlider = colorSlider(brushColor.getGreen()); blueSlider = colorSlider(brushColor.getBlue()); JSlider red = redSlider, green = greenSlider, blue = blueSlider;
        colorPreviewPanel = new JPanel(); JPanel colorPreview = colorPreviewPanel; colorPreview.setPreferredSize(new Dimension(22, 22)); colorPreview.setBackground(brushColor); colorPreview.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY)); colorPreview.setToolTipText("Current color");
        javax.swing.event.ChangeListener rgb = e -> { brushColor = new Color(red.getValue(), green.getValue(), blue.getValue()); colorPreview.setBackground(brushColor); color.setBackground(brushColor); };
        red.addChangeListener(rgb); green.addChangeListener(rgb); blue.addChangeListener(rgb);
        color.addActionListener(e -> { Color picked = JColorChooser.showDialog(this, "Brush color", brushColor); if (picked != null) { brushColor = picked; red.setValue(picked.getRed()); green.setValue(picked.getGreen()); blue.setValue(picked.getBlue()); colorPreview.setBackground(picked); color.setBackground(picked); } });
        red.setToolTipText("Red channel (R), 0–255"); green.setToolTipText("Green channel (G), 0–255"); blue.setToolTipText("Blue channel (B), 0–255");
        JPanel rgbPanel = new JPanel(new GridLayout(3, 2, 1, 0)); rgbPanel.setOpaque(false); rgbPanel.add(controlIconLabel("red", "Red channel (R)")); rgbPanel.add(red); rgbPanel.add(controlIconLabel("green", "Green channel (G)")); rgbPanel.add(green); rgbPanel.add(controlIconLabel("blue", "Blue channel (B)")); rgbPanel.add(blue); rgbPanel.setPreferredSize(new Dimension(72, 36));
        JButton zoomOut = new JButton("−"); zoomOut.setToolTipText("Zoom out"); zoomOut.addActionListener(e -> canvas.setZoom(canvas.getZoom() / 1.15));
        zoomValueLabel = new JLabel("100%"); zoomValueLabel.setHorizontalAlignment(SwingConstants.CENTER); zoomValueLabel.setPreferredSize(new Dimension(48, 24)); zoomValueLabel.setToolTipText("Current zoom; use the mouse wheel over the canvas to zoom at the pointer");
        JButton zoomIn = new JButton("+"); zoomIn.setToolTipText("Zoom in"); zoomIn.addActionListener(e -> canvas.setZoom(canvas.getZoom() * 1.15));
        JButton fit = new JButton("Fit", new EditorIcon("fit")); fit.setToolTipText("Fit image to available canvas");
        fit.addActionListener(e -> { int vw = Math.max(1, canvasScroll.getViewport().getWidth() - 40), vh = Math.max(1, canvasScroll.getViewport().getHeight() - 40); double fitZoom = Math.min((double) vw / image.getWidth(), (double) vh / image.getHeight()); canvas.setZoom(fitZoom); updateStatus("Fit view"); });
        JButton shadow = new JButton("Shadow", new EditorIcon("shadow")); shadow.setToolTipText("Add shadow to selected layer"); shadow.addActionListener(e -> applyShadow());
        shapeType.setPreferredSize(new Dimension(175, 29)); cropType.setPreferredSize(new Dimension(115, 25)); size.setPreferredSize(new Dimension(70, 22));
        adjustments.setPreferredSize(new Dimension(95, 36));
        JPanel toolRow = toolbarRow(); toolRow.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        toolRow.add(undoButton); toolRow.add(redoButton); toolRow.add(new JSeparator(SwingConstants.VERTICAL));
        toolRow.add(move); toolRow.add(brush); toolRow.add(brushType); toolRow.add(erase); toolRow.add(text); toolRow.add(select); toolRow.add(eyedropper); toolRow.add(fill); toolRow.add(gradient); toolRow.add(rotate);

        JPanel optionsRow = toolbarRow(); optionsRow.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        optionsRow.add(new JLabel("Size")); optionsRow.add(size); optionsRow.add(crop); optionsRow.add(cropType); optionsRow.add(shape); optionsRow.add(shapeType); optionsRow.add(shadow);
        optionsRow.add(color); optionsRow.add(colorPreview); optionsRow.add(rgbPanel);
        optionsRow.add(adjustments); optionsRow.add(applyAdjustments);
        MouseAdapter hidePickerLens = new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { canvas.hidePickerLens(); } };
        JPanel zoomGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0)); zoomGroup.setOpaque(false); zoomGroup.add(zoomOut); zoomGroup.add(zoomValueLabel); zoomGroup.add(zoomIn); zoomGroup.add(fit);
        zoomControlsPanel = zoomGroup;

        bar.add(toolRow); bar.add(optionsRow);
        addMouseListenerToTree(bar, hidePickerLens);
        addMouseListenerToTree(zoomControlsPanel, hidePickerLens);
        return bar;
    }

    private JPanel toolbarRow() { JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT); return row; }
    private JLabel controlIconLabel(String type, String description) { JLabel label = new JLabel(new ControlIcon(type)); label.setToolTipText(description); label.getAccessibleContext().setAccessibleName(description); label.setHorizontalAlignment(SwingConstants.CENTER); return label; }
    private void addMouseListenerToTree(Component component, MouseListener listener) { component.addMouseListener(listener); if (component instanceof Container container) for (Component child : container.getComponents()) addMouseListenerToTree(child, listener); }

    private JSlider colorSlider(int value) { JSlider slider = new JSlider(0, 255, value); slider.setToolTipText("RGB color channel"); slider.setFocusable(false); return slider; }

    private void updateStatus(String message) { status.setText(message + "   |   Canvas: " + image.getWidth() + " × " + image.getHeight() + " px"); }

    private void updateToolCursor() {
        int cursorType = tool == Tool.MOVE ? Cursor.DEFAULT_CURSOR : tool == Tool.TEXT ? Cursor.TEXT_CURSOR : tool == Tool.ROTATE ? Cursor.HAND_CURSOR : Cursor.CROSSHAIR_CURSOR;
        canvas.setCursor(Cursor.getPredefinedCursor(cursorType));
    }

    private void updateHistoryButtons() { if (undoButton != null) undoButton.setEnabled(undo.canUndo()); if (redoButton != null) redoButton.setEnabled(undo.canRedo()); }

    private void syncControlValues() {
        if (redSlider != null) { redSlider.setValue(brushColor.getRed()); greenSlider.setValue(brushColor.getGreen()); blueSlider.setValue(brushColor.getBlue()); }
        if (colorPreviewPanel != null) colorPreviewPanel.setBackground(brushColor); if (colorButton != null) colorButton.setBackground(brushColor);
        MovableItem selected = layerList.getSelectedValue(); if (selected != null) opacitySlider.setValue(Math.round(selected.opacity * 100));
    }

    private void saveWorkspace() {
        if (currentWorkspaceName == null || currentWorkspaceName.isBlank()) { String name = JOptionPane.showInputDialog(this, "Workspace name:", "Save Workspace", JOptionPane.PLAIN_MESSAGE); if (name == null || name.isBlank()) return; currentWorkspaceName = name; }
        saveWorkspaceNamed(currentWorkspaceName);
    }

    private void saveWorkspaceNamed(String workspaceName) {
        String name = workspaceName.trim().replaceAll("[^a-zA-Z0-9 _-]", "_"); if (name.isBlank()) return; currentWorkspaceName = name; File directory = workspaceDirectory(); File file = new File(directory, name + ".workspace");
        try { File imageFile = new File(file.getAbsolutePath() + ".png"); ImageIO.write(compositeImage(), "png", imageFile); try (FileOutputStream output = new FileOutputStream(file)) { Properties values = captureWorkspace(); values.setProperty("imageFile", imageFile.getName()); values.store(output, "Photoshop Studio Workspace"); } saveWorkspacePreferences(); undo.discardAllEdits(); JOptionPane.showMessageDialog(this, "Workspace saved to:\n" + file.getAbsolutePath(), "Workspace", JOptionPane.INFORMATION_MESSAGE); updateStatus("Workspace saved"); } catch (IOException ex) { showError(ex); }
        refreshSavedWorkspacesMenu();
    }

    private void newWorkspace() { String name = JOptionPane.showInputDialog(this, "New workspace name:", "New Workspace", JOptionPane.PLAIN_MESSAGE); if (name == null || name.isBlank()) return; BufferedImage currentImage = copy(image); List<MovableItem> currentItems = snapshotItems(); resetWorkspace(); image = currentImage; movableItems.clear(); movableItems.addAll(currentItems); refreshLayers(); canvas.revalidate(); canvas.repaint(); currentWorkspaceName = name; saveWorkspaceNamed(name); updateStatus("New workspace created"); }

    private File workspaceDirectory() { File directory = new File(System.getProperty("user.home"), "PhotoshopStudioWorkspaces"); if (!directory.exists()) directory.mkdirs(); return directory; }
    private void refreshSavedWorkspacesMenu() { if (savedWorkspacesMenu == null) return; savedWorkspacesMenu.removeAll(); File[] files = workspaceDirectory().listFiles((directory, name) -> name.toLowerCase().endsWith(".workspace")); if (files == null || files.length == 0) { JMenuItem empty = new JMenuItem("No saved workspaces"); empty.setEnabled(false); savedWorkspacesMenu.add(empty); return; } java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER)); for (File file : files) { JMenuItem item = new JMenuItem(file.getName().substring(0, file.getName().length() - ".workspace".length())); item.addActionListener(e -> loadWorkspaceFile(file)); savedWorkspacesMenu.add(item); } }
    private void loadWorkspaceFile(File file) { try (FileInputStream input = new FileInputStream(file)) { Properties values = new Properties(); values.load(input); applyWorkspace(values); String imageName = values.getProperty("imageFile"); if (imageName != null) { File imageFile = new File(file.getParentFile(), imageName); BufferedImage loadedImage = ImageIO.read(imageFile); if (loadedImage != null) { image = loadedImage; movableItems.clear(); layerModel.clear(); undo.discardAllEdits(); canvas.revalidate(); canvas.repaint(); } } String fileName = file.getName(); currentWorkspaceName = fileName.toLowerCase().endsWith(".workspace") ? fileName.substring(0, fileName.length() - ".workspace".length()) : fileName; updateStatus("Workspace loaded"); } catch (IOException | NumberFormatException ex) { showError(ex); } }
    private void deleteWorkspace() { JFileChooser chooser = new JFileChooser(workspaceDirectory()); chooser.setDialogTitle("Delete Workspace"); if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return; File file = chooser.getSelectedFile(); if (!file.getName().toLowerCase().endsWith(".workspace")) return; int choice = JOptionPane.showConfirmDialog(this, "Delete workspace '" + file.getName() + "'?", "Delete Workspace", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE); if (choice != JOptionPane.YES_OPTION) return; try { Properties values = new Properties(); if (file.isFile()) try (FileInputStream input = new FileInputStream(file)) { values.load(input); } boolean deletedWorkspace = file.delete(), deletedImage = true; String imageName = values.getProperty("imageFile"); if (imageName != null) { File imageFile = new File(file.getParentFile(), imageName); deletedImage = !imageFile.exists() || imageFile.delete(); } if (!deletedWorkspace || !deletedImage) throw new IOException("The workspace could not be deleted."); refreshSavedWorkspacesMenu(); updateStatus("Workspace deleted"); } catch (IOException ex) { showError(ex); } }

    private void loadWorkspace() {
        Properties values = new Properties(); values.setProperty("windowWidth", workspacePreferences.getInt("windowWidth", 1100) + ""); values.setProperty("windowHeight", workspacePreferences.getInt("windowHeight", 750) + ""); values.setProperty("windowX", workspacePreferences.getInt("windowX", getX()) + ""); values.setProperty("windowY", workspacePreferences.getInt("windowY", getY()) + ""); values.setProperty("zoom", workspacePreferences.getDouble("zoom", 1.0) + ""); values.setProperty("brushSize", workspacePreferences.getInt("brushSize", brushSize) + ""); values.setProperty("red", workspacePreferences.getInt("red", brushColor.getRed()) + ""); values.setProperty("green", workspacePreferences.getInt("green", brushColor.getGreen()) + ""); values.setProperty("blue", workspacePreferences.getInt("blue", brushColor.getBlue()) + ""); values.setProperty("tool", workspacePreferences.get("tool", Tool.BRUSH.name())); values.setProperty("cropType", workspacePreferences.getInt("cropType", 0) + ""); applyWorkspace(values);
    }

    private Properties captureWorkspace() { Properties values = new Properties(); values.setProperty("windowWidth", getWidth() + ""); values.setProperty("windowHeight", getHeight() + ""); values.setProperty("windowX", getX() + ""); values.setProperty("windowY", getY() + ""); values.setProperty("zoom", canvas.getZoom() + ""); values.setProperty("brushSize", brushSize + ""); values.setProperty("red", brushColor.getRed() + ""); values.setProperty("green", brushColor.getGreen() + ""); values.setProperty("blue", brushColor.getBlue() + ""); values.setProperty("tool", tool.name()); values.setProperty("cropType", cropTypeSelector == null ? "0" : cropTypeSelector.getSelectedIndex() + ""); return values; }
    private void saveWorkspacePreferences() { Properties values = captureWorkspace(); for (String key : values.stringPropertyNames()) workspacePreferences.put(key, values.getProperty(key)); }
    private void applyWorkspace(Properties values) { setSize(Integer.parseInt(values.getProperty("windowWidth", "1100")), Integer.parseInt(values.getProperty("windowHeight", "750"))); if (values.getProperty("windowX") != null) setLocation(Integer.parseInt(values.getProperty("windowX")), Integer.parseInt(values.getProperty("windowY", "0"))); brushSize = Integer.parseInt(values.getProperty("brushSize", "18")); if (brushSizeSlider != null) brushSizeSlider.setValue(brushSize); brushColor = new Color(Integer.parseInt(values.getProperty("red", "0")), Integer.parseInt(values.getProperty("green", "0")), Integer.parseInt(values.getProperty("blue", "0"))); syncControlValues(); double savedZoom = Double.parseDouble(values.getProperty("zoom", "1.0")); canvas.setZoom(savedZoom); if (cropTypeSelector != null) cropTypeSelector.setSelectedIndex(Math.max(0, Math.min(cropTypeSelector.getItemCount() - 1, Integer.parseInt(values.getProperty("cropType", "0"))))); try { tool = Tool.valueOf(values.getProperty("tool", Tool.BRUSH.name())); } catch (IllegalArgumentException ignored) { tool = Tool.BRUSH; } updateToolCursor(); }
    private void loadWorkspaceFromFile() { JFileChooser chooser = new JFileChooser(workspaceDirectory()); chooser.setDialogTitle("Load Workspace"); if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return; loadWorkspaceFile(chooser.getSelectedFile()); JOptionPane.showMessageDialog(this, "Workspace loaded.", "Workspace", JOptionPane.INFORMATION_MESSAGE); }

    private void resetWorkspace() {
        try { workspacePreferences.clear(); } catch (java.util.prefs.BackingStoreException ignored) { } setSize(1100, 750); brushSize = 18; brushColor = Color.BLACK; canvas.setZoom(1.0); if (brushSizeSlider != null) brushSizeSlider.setValue(brushSize); if (cropTypeSelector != null) cropTypeSelector.setSelectedIndex(0); tool = Tool.BRUSH; syncControlValues(); updateToolCursor(); updateStatus("Workspace reset");
    }

    private void showImageInformation() {
        long pixels = (long) image.getWidth() * image.getHeight(); long red = 0, green = 0, blue = 0; int visibleLayers = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) { int rgb = image.getRGB(x, y); red += (rgb >>> 16) & 255; green += (rgb >>> 8) & 255; blue += rgb & 255; }
        for (MovableItem layer : movableItems) if (layer.visible) visibleLayers++;
        double averageRed = pixels == 0 ? 0 : (double) red / pixels, averageGreen = pixels == 0 ? 0 : (double) green / pixels, averageBlue = pixels == 0 ? 0 : (double) blue / pixels;
        MovableItem selected = layerList.getSelectedValue(); String selectedInfo = "None";
        if (selected != null) {
            long layerPixels = (long) selected.image.getWidth() * selected.image.getHeight(), nonTransparent = 0, layerRed = 0, layerGreen = 0, layerBlue = 0;
            for (int y = 0; y < selected.image.getHeight(); y++) for (int x = 0; x < selected.image.getWidth(); x++) { int rgb = selected.image.getRGB(x, y), alpha = (rgb >>> 24) & 255; if (alpha > 0) { nonTransparent++; layerRed += (rgb >>> 16) & 255; layerGreen += (rgb >>> 8) & 255; layerBlue += rgb & 255; } }
            long visiblePixels = Math.max(1, nonTransparent); selectedInfo = selected.name + "\n" + "  Size: " + selected.image.getWidth() + " × " + selected.image.getHeight() + " px\n" + "  Position: " + selected.x + ", " + selected.y + "\n" + "  Pixels: " + String.format("%,d", layerPixels) + " total, " + String.format("%,d", nonTransparent) + " visible\n" + "  Average RGB: " + (int) (layerRed / visiblePixels) + ", " + (int) (layerGreen / visiblePixels) + ", " + (int) (layerBlue / visiblePixels) + "\n" + "  Opacity: " + Math.round(selected.opacity * 100) + "%";
        }
        String details = "Canvas: " + image.getWidth() + " × " + image.getHeight() + " px\n" + "Pixels: " + String.format("%,d", pixels) + "\n" + "Color model: " + image.getColorModel().getNumColorComponents() + " channels, " + image.getColorModel().getPixelSize() + " bits\n" + "Layers: " + movableItems.size() + " total, " + visibleLayers + " visible\n" + "Selected layer:\n" + selectedInfo + "\n" + "Canvas average RGB: " + (int) averageRed + ", " + (int) averageGreen + ", " + (int) averageBlue + "\n" + "Zoom: " + Math.round(canvas.getZoom() * 100) + "%";
        JTextArea area = new JTextArea(details); area.setEditable(false); area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JOptionPane.showMessageDialog(this, area, "Image Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearCanvas() {
        BufferedImage before = copy(image); Graphics2D g = image.createGraphics(); g.setComposite(AlphaComposite.Clear); g.fillRect(0, 0, image.getWidth(), image.getHeight()); g.dispose();
        addImageEdit(before, copy(image)); canvas.repaint(); updateStatus("Canvas cleared");
    }

    private void transformImage(boolean rotate) {
        BufferedImage before = copy(image);
        int newWidth = rotate ? image.getHeight() : image.getWidth(), newHeight = rotate ? image.getWidth() : image.getHeight();
        BufferedImage transformed = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = transformed.createGraphics();
        if (rotate) { g.translate(newWidth, 0); g.rotate(Math.PI / 2); }
        else { g.translate(newWidth, 0); g.scale(-1, 1); }
        g.drawImage(image, 0, 0, null); g.dispose(); image = transformed;
        addImageEdit(before, copy(image)); canvas.revalidate(); canvas.repaint(); updateStatus(rotate ? "Rotated 90°" : "Flipped horizontally");
    }

    private void mergeImages() {
        if (movableItems.isEmpty()) { updateStatus("Nothing to merge"); return; }
        BufferedImage beforeImage = copy(image); List<MovableItem> beforeItems = snapshotItems();
        image = compositeImage(); movableItems.clear(); layerModel.clear();
        undo.addEdit(new AbstractUndoableEdit() {
            @Override public void undo() { super.undo(); image = copy(beforeImage); movableItems.clear(); movableItems.addAll(snapshotItems(beforeItems)); refreshLayers(); canvas.revalidate(); canvas.repaint(); }
            @Override public void redo() { super.redo(); image = compositeImage(); movableItems.clear(); layerModel.clear(); canvas.revalidate(); canvas.repaint(); }
        });
        canvas.revalidate(); canvas.repaint(); updateStatus("Images merged");
    }

    private List<MovableItem> snapshotItems() { return snapshotItems(movableItems); }
    private List<MovableItem> snapshotItems(List<MovableItem> source) {
        List<MovableItem> result = new ArrayList<>();
        for (MovableItem item : source) { MovableItem clone = new MovableItem(copy(item.image), item.x, item.y, item.name); clone.opacity = item.opacity; clone.visible = item.visible; clone.textContent = item.textContent; clone.fontFamily = item.fontFamily; clone.fontStyle = item.fontStyle; clone.fontSize = item.fontSize; clone.textColor = item.textColor; clone.borderColor = item.borderColor; clone.borderWidth = item.borderWidth; clone.textShadow = item.textShadow; clone.shadowColor = item.shadowColor; clone.shadowOffset = item.shadowOffset; result.add(clone); }
        return result;
    }

    private void newImage(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, width, height); g.dispose();
        movableItems.clear(); layerModel.clear();
        undo.discardAllEdits(); canvas.repaint();
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try { image = ImageIO.read(chooser.getSelectedFile()); if (image == null) throw new IOException("Unsupported image format"); movableItems.clear(); layerModel.clear(); undo.discardAllEdits(); canvas.repaint(); }
        catch (IOException ex) { showError(ex); }
    }

    private void saveImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try { ImageIO.write(compositeImage(), "png", chooser.getSelectedFile()); undo.discardAllEdits(); } catch (IOException ex) { showError(ex); }
    }

    private void attemptClose() {
        if (!undo.canUndo()) { dispose(); return; }
        String[] options = {"Save Image As...", "Discard", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "You have unsaved edits. Choose how to save before closing.", "Unsaved Edits", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (choice == 0) { saveImage(); if (!undo.canUndo()) dispose(); }
        else if (choice == 1) { dispose(); }
    }

    private void placeOverlay() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage overlay = ImageIO.read(chooser.getSelectedFile());
            if (overlay == null) throw new IOException("Unsupported image format");
            addOverlayImage(overlay, image.getWidth() / 2, image.getHeight() / 2, "Image Overlay");
        } catch (IOException ex) { showError(ex); }
    }

    private void addOverlayImage(BufferedImage overlay, int centerX, int centerY, String name) {
        try {
            int drawWidth = overlay.getWidth(), drawHeight = overlay.getHeight();
            if (drawWidth > image.getWidth() || drawHeight > image.getHeight()) {
                double scale = Math.min((double) image.getWidth() / drawWidth, (double) image.getHeight() / drawHeight);
                drawWidth = Math.max(1, (int) (drawWidth * scale)); drawHeight = Math.max(1, (int) (drawHeight * scale));
            }
            if (drawWidth != overlay.getWidth() || drawHeight != overlay.getHeight()) {
                BufferedImage scaled = new BufferedImage(drawWidth, drawHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D sg = scaled.createGraphics(); sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); sg.drawImage(overlay, 0, 0, drawWidth, drawHeight, null); sg.dispose(); overlay = scaled;
            }
            int x = Math.max(0, Math.min(image.getWidth() - overlay.getWidth(), centerX - overlay.getWidth() / 2));
            int y = Math.max(0, Math.min(image.getHeight() - overlay.getHeight(), centerY - overlay.getHeight() / 2));
            MovableItem item = new MovableItem(overlay, x, y, name); registerItem(item);
            undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); unregisterItem(item); canvas.repaint(); } @Override public void redo() { super.redo(); registerItem(item); canvas.repaint(); } });
            canvas.repaint(); updateStatus("Image added");
        } catch (RuntimeException ex) { showError(ex); }
    }

    private void showError(Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Operation failed", JOptionPane.ERROR_MESSAGE); }

    private void applyEffect(String effect) {
        MovableItem selected = layerList.getSelectedValue();
        if (selected != null) { applyEffectToItem(selected, effect); return; }
        BufferedImage canvasBefore = copy(image), canvasAfter = filterCopy(image, effect); image = canvasAfter;
        undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); image = copy(canvasBefore); canvas.revalidate(); canvas.repaint(); } @Override public void redo() { super.redo(); image = copy(canvasAfter); canvas.revalidate(); canvas.repaint(); } });
        canvas.revalidate(); canvas.repaint(); updateStatus("Effect applied to canvas");
    }

    private void applyEffectToItem(MovableItem item, String effect) {
        BufferedImage before = copy(item.image), after = filterCopy(item.image, effect); item.image = after;
        addItemImageEdit(item, before, copy(after)); updateStatus("Effect applied to " + item.name);
    }

    private boolean requirePortraitSelection() {
        Rectangle selected = canvas.selection;
        if (selected != null && selected.width > 1 && selected.height > 1) return true;
        JOptionPane.showMessageDialog(this, "Choose the Select tool and drag around the face, hair, or beard area first. The effect stays within that selection.", "Select a portrait area", JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    private void applyPortraitEffect(String effect, Color tint) {
        if (!requirePortraitSelection()) return;
        boolean hairColoring=effect.equals("hairColor"), beardColoring=effect.equals("beardColor");
        MovableItem layer = layerList.getSelectedValue();
        BufferedImage target = layer == null ? image : layer.image;
        int originX = layer == null ? 0 : layer.x, originY = layer == null ? 0 : layer.y;
        Rectangle area = canvas.selection.intersection(new Rectangle(originX, originY, target.getWidth(), target.getHeight()));
        if (area.width < 2 || area.height < 2) { updateStatus("Portrait selection does not overlap the active image"); return; }
        BufferedImage before = copy(target), after = copy(target);
        int left = area.x - originX, top = area.y - originY, right = left + area.width, bottom = top + area.height;
        for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
            int pixel = before.getRGB(x, y), alpha = pixel & 0xff000000;
            int r=(pixel>>>16)&255, g=(pixel>>>8)&255, b=pixel&255, lum=luminance(pixel);
            if (hairColoring || beardColoring) {
                double amount=hairColoring?hairPixelMask(pixel):beardPixelMask(pixel);
                if(amount>0){float[] original=Color.RGBtoHSB(r,g,b,null),chosen=Color.RGBtoHSB(tint.getRed(),tint.getGreen(),tint.getBlue(),null);float saturation=(float)(original[1]+(Math.max(.55f,chosen[1])-original[1])*amount);int recolored=Color.HSBtoRGB(chosen[0],saturation,original[2]);r=(recolored>>>16)&255;g=(recolored>>>8)&255;b=recolored&255;}
            } else if (effect.equals("smoothSkin")) {
                if (lum > 48 && lum < 238) {
                    int totalR=0,totalG=0,totalB=0,count=0,edge=0;
                    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int sx=Math.max(0,Math.min(before.getWidth()-1,x+dx)),sy=Math.max(0,Math.min(before.getHeight()-1,y+dy)),sample=before.getRGB(sx,sy);totalR+=(sample>>>16)&255;totalG+=(sample>>>8)&255;totalB+=sample&255;edge+=Math.abs(lum-luminance(sample));count++;}
                    double mix=.62*Math.max(0,1-(edge/(double)count)/58.0);
                    r=clamp((int)(r*(1-mix)+(totalR/(double)count)*mix));g=clamp((int)(g*(1-mix)+(totalG/(double)count)*mix));b=clamp((int)(b*(1-mix)+(totalB/(double)count)*mix));
                }
            } else if (effect.equals("warmSkin")) {
                r=clamp(r+10); g=clamp(g+3); b=clamp(b-7);
            }
            after.setRGB(x,y,alpha|(r<<16)|(g<<8)|b);
        }
        if (layer == null) { image=after; addImageEdit(before,copy(after)); }
        else { layer.image=after; addItemImageEdit(layer,before,copy(after)); }
        canvas.repaint(); updateStatus("Portrait effect applied to selected area");
    }

    private boolean isLikelySkin(int argb) {
        int r=(argb>>>16)&255,g=(argb>>>8)&255,b=argb&255;
        int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
        double y=.299*r+.587*g+.114*b,cb=128-.168736*r-.331264*g+.5*b,cr=128+.5*r-.418688*g-.081312*b;
        boolean ycbcr=y>70&&cb>=75&&cb<=130&&cr>=132&&cr<=180;
        boolean rgb=y>95&&r>95&&g>35&&b>15&&max-min>15&&r>g&&r>b&&Math.abs(r-g)>12;
        return ycbcr||rgb;
    }

    private double hairPixelMask(int argb) {
        if(((argb>>>24)&255)<32||isLikelySkin(argb))return 0;
        int r=(argb>>>16)&255,g=(argb>>>8)&255,b=argb&255,lum=luminance(argb);
        double dark=Math.max(0,Math.min(1,(220-lum)/145.0));
        int range=Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b));
        double gray=range<28&&lum>=45&&lum<190?.58:0;
        boolean brown=r>=g&&g>=b&&r-g<105&&g-b>5&&lum<205;
        double brownMask=brown?Math.min(.82,(g-b+24)/100.0):0;
        return Math.max(dark,Math.max(gray,brownMask));
    }

    private double beardPixelMask(int argb) {
        if(((argb>>>24)&255)<32||isLikelySkin(argb))return 0;
        int lum=luminance(argb);
        return Math.max(0,Math.min(1,(190-lum)/120.0));
    }

    private void perspectiveCrop() {
        int w = image.getWidth(), h = image.getHeight(); JSpinner[] fields = new JSpinner[10]; JPanel form = new JPanel(new GridLayout(0, 4, 5, 5));
        String[] labels = {"Top-left X", "Top-left Y", "Top-right X", "Top-right Y", "Bottom-right X", "Bottom-right Y", "Bottom-left X", "Bottom-left Y", "Output width", "Output height"}; int[] defaults = {0, 0, w, 0, w, h, 0, h, w, h};
        for (int i = 0; i < fields.length; i++) { fields[i] = new JSpinner(new SpinnerNumberModel(defaults[i], 0, 10000, 1)); form.add(new JLabel(labels[i])); form.add(fields[i]); }
        if (JOptionPane.showConfirmDialog(this, form, "Perspective Crop", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        double[][] corners = {{(Integer) fields[0].getValue(), (Integer) fields[1].getValue()}, {(Integer) fields[2].getValue(), (Integer) fields[3].getValue()}, {(Integer) fields[4].getValue(), (Integer) fields[5].getValue()}, {(Integer) fields[6].getValue(), (Integer) fields[7].getValue()}}; int outputWidth = (Integer) fields[8].getValue(), outputHeight = (Integer) fields[9].getValue();
        if (outputWidth < 2 || outputHeight < 2) return; BufferedImage before = copy(image), result = perspectiveWarp(image, corners, outputWidth, outputHeight); image = result; addImageEdit(before, copy(result)); canvas.revalidate(); canvas.repaint(); updateStatus("Perspective crop applied");
    }

    private BufferedImage perspectiveWarp(BufferedImage source, double[][] corners, int width, int height) {
        double[][] matrix = new double[8][8]; double[] values = new double[8]; double[][] destination = {{0, 0}, {width - 1, 0}, {width - 1, height - 1}, {0, height - 1}};
        for (int i = 0; i < 4; i++) { double u = destination[i][0], v = destination[i][1], sx = corners[i][0], sy = corners[i][1]; int row = i * 2; matrix[row] = new double[]{u, v, 1, 0, 0, 0, -u * sx, -v * sx}; values[row] = sx; matrix[row + 1] = new double[]{0, 0, 0, u, v, 1, -u * sy, -v * sy}; values[row + 1] = sy; }
        double[] homography = solveLinearSystem(matrix, values); BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) { double denominator = homography[6] * x + homography[7] * y + 1; if (Math.abs(denominator) < 0.000001) continue; int sourceX = (int) Math.round((homography[0] * x + homography[1] * y + homography[2]) / denominator), sourceY = (int) Math.round((homography[3] * x + homography[4] * y + homography[5]) / denominator); if (sourceX >= 0 && sourceY >= 0 && sourceX < source.getWidth() && sourceY < source.getHeight()) result.setRGB(x, y, source.getRGB(sourceX, sourceY)); }
        return result;
    }

    private double[] solveLinearSystem(double[][] input, double[] inputValues) {
        double[][] a = new double[8][9]; for (int r = 0; r < 8; r++) { System.arraycopy(input[r], 0, a[r], 0, 8); a[r][8] = inputValues[r]; }
        for (int column = 0; column < 8; column++) { int pivot = column; for (int row = column + 1; row < 8; row++) if (Math.abs(a[row][column]) > Math.abs(a[pivot][column])) pivot = row; double[] swap = a[column]; a[column] = a[pivot]; a[pivot] = swap; double divisor = a[column][column]; if (Math.abs(divisor) < 0.0000001) continue; for (int j = column; j < 9; j++) a[column][j] /= divisor; for (int row = 0; row < 8; row++) if (row != column) { double factor = a[row][column]; for (int j = column; j < 9; j++) a[row][j] -= factor * a[column][j]; } }
        double[] result = new double[8]; for (int i = 0; i < 8; i++) result[i] = a[i][8]; return result;
    }

    private BufferedImage filterCopy(BufferedImage source, String effect) {
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage result = copy(source);

        // Spatial filters are deliberately separate: blur smooths all pixels,
        // denoise removes isolated outliers, while the sharpening filters target edges.
        if (effect.equals("blur")) return convolve(source, new float[]{1,2,1,2,4,2,1,2,1}, 16f);
        if (effect.equals("reduceNoise")) return medianFilter(source);
        if (effect.equals("sharpen")) return convolve(source, new float[]{0,-1,0,-1,5,-1,0,-1,0}, 1f);
        if (effect.equals("edge")) return convolve(source, new float[]{-1,-1,-1,-1,8,-1,-1,-1,-1}, 1f);
        if (effect.equals("emboss")) return convolve(source, new float[]{-2,-1,0,-1,1,1,0,1,2}, 1f);
        if (effect.equals("pixelate")) {
            int block = 10;
            for (int by=0; by<height; by+=block) for (int bx=0; bx<width; bx+=block) {
                long rr=0,gg=0,bb=0; int count=0;
                for (int y=by; y<Math.min(by+block,height); y++) for (int x=bx; x<Math.min(bx+block,width); x++) { int p=source.getRGB(x,y); rr+=(p>>>16)&255; gg+=(p>>>8)&255; bb+=p&255; count++; }
                int alpha=source.getRGB(bx,by)&0xff000000, color=alpha|((int)(rr/count)<<16)|((int)(gg/count)<<8)|(int)(bb/count);
                for (int y=by; y<Math.min(by+block,height); y++) for (int x=bx; x<Math.min(bx+block,width); x++) result.setRGB(x,y,color);
            }
            return result;
        }
        if (effect.equals("glow")) {
            BufferedImage blurred=convolve(source,new float[]{1,2,1,2,4,2,1,2,1},16f);
            for(int y=0;y<height;y++) for(int x=0;x<width;x++) { int p=source.getRGB(x,y), q=blurred.getRGB(x,y); int lum=luminance(p); double amount=Math.max(0,(lum-105)/150.0)*.65; int r=screen((p>>>16)&255,(q>>>16)&255,amount),g=screen((p>>>8)&255,(q>>>8)&255,amount),b=screen(p&255,q&255,amount); result.setRGB(x,y,(p&0xff000000)|(r<<16)|(g<<8)|b); }
            return result;
        }

        int minLum=255,maxLum=0;
        if(effect.equals("autoEnhance")) for(int y=0;y<height;y++) for(int x=0;x<width;x++){int l=luminance(source.getRGB(x,y)); minLum=Math.min(minLum,l); maxLum=Math.max(maxLum,l);}
        for (int y=0; y<height; y++) for (int x=0; x<width; x++) {
            int argb=source.getRGB(x,y), a=argb&0xff000000, r=(argb>>>16)&255, g=(argb>>>8)&255, b=argb&255, lum=luminance(argb);
            switch(effect) {
                case "grayscale" -> r=g=b=lum;
                case "invert" -> {r=255-r;g=255-g;b=255-b;}
                case "bright" -> {r=clamp(r+25);g=clamp(g+25);b=clamp(b+25);}
                case "contrast" -> {r=clamp((int)((r-128)*1.35+128));g=clamp((int)((g-128)*1.35+128));b=clamp((int)((b-128)*1.35+128));}
                case "sepia" -> {int nr=clamp((int)(r*.393+g*.769+b*.189)),ng=clamp((int)(r*.349+g*.686+b*.168)),nb=clamp((int)(r*.272+g*.534+b*.131));r=nr;g=ng;b=nb;}
                case "posterize" -> {r=(r/64)*64;g=(g/64)*64;b=(b/64)*64;}
                case "threshold" -> r=g=b=lum>=128?255:0;
                case "cool" -> {r=clamp((int)(r*.92));g=clamp((int)(g*1.01));b=clamp((int)(b*1.16));}
                case "warm" -> {r=clamp((int)(r*1.14));g=clamp((int)(g*1.02));b=clamp((int)(b*.88));}
                case "saturation" -> {int avg=(r+g+b)/3;r=clamp((int)(avg+(r-avg)*1.4));g=clamp((int)(avg+(g-avg)*1.4));b=clamp((int)(avg+(b-avg)*1.4));}
                case "vignette" -> {double dx=(x-(width-1)/2.0)/(width/2.0),dy=(y-(height-1)/2.0)/(height/2.0),f=Math.max(.18,1-.58*(dx*dx+dy*dy));r=clamp((int)(r*f));g=clamp((int)(g*f));b=clamp((int)(b*f));}
                case "autoEnhance" -> {int stretched=maxLum==minLum?lum:clamp((lum-minLum)*255/(maxLum-minLum));double f=lum==0?1:stretched/(double)lum;r=clamp((int)(r*f));g=clamp((int)(g*f));b=clamp((int)(b*f));}
                case "exposure" -> {r=clamp((int)(r*1.55));g=clamp((int)(g*1.55));b=clamp((int)(b*1.55));}
                case "highlights" -> {double t=Math.max(0,(lum-112)/143.0),f=1+.34*t;r=clamp((int)(lum+(r-lum)*f+32*t));g=clamp((int)(lum+(g-lum)*f+32*t));b=clamp((int)(lum+(b-lum)*f+32*t));}
                case "shadows" -> {double t=Math.max(0,(145-lum)/145.0),lift=78*t*t;r=clamp((int)(r+lift));g=clamp((int)(g+lift));b=clamp((int)(b+lift));}
                case "gamma" -> {r=clamp((int)(255*Math.pow(r/255.0,1.65)));g=clamp((int)(255*Math.pow(g/255.0,1.65)));b=clamp((int)(255*Math.pow(b/255.0,1.65)));}
                // Colorize lays a cyan tint over existing color; duotone remaps luminance to two inks.
                case "colorize" -> {r=clamp((int)(r*.68+lum*.08));g=clamp((int)(g*.68+lum*.18+30));b=clamp((int)(b*.68+lum*.32+45));}
                case "duotone" -> {double t=lum/255.0;r=clamp((int)(18+237*t));g=clamp((int)(24+178*t));b=clamp((int)(62+105*t));}
                case "solarize" -> {r=r>128?255-r:r;g=g>128?255-g:g;b=b>128?255-b:b;}
                case "filmGrain" -> {int hash=x*374761393+y*668265263;hash=(hash^(hash>>>13))*1274126177;int grain=((hash^(hash>>>16))&31)-15;r=clamp(r+grain);g=clamp(g+grain);b=clamp(b+grain);}
                case "smartSharpen" -> {int neighborhood=luminance(source.getRGB(Math.max(0,x-1),y))+luminance(source.getRGB(Math.min(width-1,x+1),y))+luminance(source.getRGB(x,Math.max(0,y-1)))+luminance(source.getRGB(x,Math.min(height-1,y+1)));int detail=lum*4-neighborhood;if(Math.abs(detail)>18){r=clamp(r+detail/2);g=clamp(g+detail/2);b=clamp(b+detail/2);}}
                case "clarity" -> {int neighbors=luminance(source.getRGB(Math.max(0,x-1),y))+luminance(source.getRGB(Math.min(width-1,x+1),y))+luminance(source.getRGB(x,Math.max(0,y-1)))+luminance(source.getRGB(x,Math.min(height-1,y+1)));int local=neighbors/4,delta=lum-local;r=clamp(r+delta/2);g=clamp(g+delta/2);b=clamp(b+delta/2);}
                default -> { }
            }
            result.setRGB(x,y,a|(r<<16)|(g<<8)|b);
        }
        return result;
    }

    private int luminance(int argb) { return (((argb>>>16)&255)*30+((argb>>>8)&255)*59+(argb&255)*11)/100; }
    private int screen(int base,int glow,double amount) { int value=255-(int)((255-base)*(255-glow)/255.0); return clamp((int)(base+(value-base)*amount)); }
    private BufferedImage convolve(BufferedImage source,float[] kernel,float divisor) { BufferedImage result=copy(source); for(int y=1;y<source.getHeight()-1;y++)for(int x=1;x<source.getWidth()-1;x++){double r=0,g=0,b=0;int k=0;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int p=source.getRGB(x+dx,y+dy);float weight=kernel[k++]/divisor;r+=((p>>>16)&255)*weight;g+=((p>>>8)&255)*weight;b+=(p&255)*weight;}int alpha=source.getRGB(x,y)&0xff000000;result.setRGB(x,y,alpha|(clamp((int)Math.round(r))<<16)|(clamp((int)Math.round(g))<<8)|clamp((int)Math.round(b)));}return result; }
    private BufferedImage medianFilter(BufferedImage source) { BufferedImage result=copy(source);for(int y=1;y<source.getHeight()-1;y++)for(int x=1;x<source.getWidth()-1;x++){int[] rr=new int[9],gg=new int[9],bb=new int[9];int i=0;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int p=source.getRGB(x+dx,y+dy);rr[i]=(p>>>16)&255;gg[i]=(p>>>8)&255;bb[i++]=p&255;}java.util.Arrays.sort(rr);java.util.Arrays.sort(gg);java.util.Arrays.sort(bb);result.setRGB(x,y,(source.getRGB(x,y)&0xff000000)|(rr[4]<<16)|(gg[4]<<8)|bb[4]);}return result; }

    private int clamp(int value) { return Math.max(0, Math.min(255, value)); }

    private void addImageEdit(BufferedImage before, BufferedImage after) {
        undo.addEdit(new AbstractUndoableEdit() {
            @Override public void undo() { super.undo(); image = copy(before); canvas.revalidate(); canvas.repaint(); }
            @Override public void redo() { super.redo(); image = copy(after); canvas.revalidate(); canvas.repaint(); }
        });
    }

    private void resizeItem(MovableItem item) {
        JSpinner width = new JSpinner(new SpinnerNumberModel(item.image.getWidth(), 1, 10000, 1));
        JSpinner height = new JSpinner(new SpinnerNumberModel(item.image.getHeight(), 1, 10000, 1));
        JCheckBox keepRatio = new JCheckBox("Keep proportions", true);
        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6)); form.add(new JLabel("Width")); form.add(width); form.add(new JLabel("Height")); form.add(height); form.add(new JLabel()); form.add(keepRatio);
        if (JOptionPane.showConfirmDialog(this, form, "Resize Layer", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        int newWidth = (Integer) width.getValue(), newHeight = (Integer) height.getValue();
        if (keepRatio.isSelected()) { double ratio = (double) item.image.getHeight() / item.image.getWidth(); newHeight = Math.max(1, (int) (newWidth * ratio)); }
        BufferedImage before = copy(item.image); BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics(); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.drawImage(item.image, 0, 0, newWidth, newHeight, null); g.dispose(); item.image = resized; addItemImageEdit(item, before, copy(resized));
    }

    private void transformItem(MovableItem item, String operation) {
        BufferedImage before = copy(item.image); int w = item.image.getWidth(), h = item.image.getHeight();
        BufferedImage result = new BufferedImage(operation.equals("rotate") ? h : w, operation.equals("rotate") ? w : h, BufferedImage.TYPE_INT_ARGB); Graphics2D g = result.createGraphics();
        if (operation.equals("rotate")) { g.translate(h, 0); g.rotate(Math.PI / 2); }
        else if (operation.equals("flipH")) { g.translate(w, 0); g.scale(-1, 1); }
        else { g.translate(0, h); g.scale(1, -1); }
        g.drawImage(item.image, 0, 0, null); g.dispose(); item.image = result; addItemImageEdit(item, before, copy(result));
    }

    private void rotateItemFreely(MovableItem item) {
        JSpinner angle = new JSpinner(new SpinnerNumberModel(15, -360, 360, 1));
        JPanel form = new JPanel(new GridLayout(1, 2, 6, 6)); form.add(new JLabel("Angle (degrees)")); form.add(angle);
        if (JOptionPane.showConfirmDialog(this, form, "Rotate Layer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        double radians = Math.toRadians((Integer) angle.getValue()); if (Math.abs(radians) < 0.0001) return;
        BufferedImage before = copy(item.image); int oldX = item.x, oldY = item.y, w = item.image.getWidth(), h = item.image.getHeight();
        double sin = Math.abs(Math.sin(radians)), cos = Math.abs(Math.cos(radians)); int newWidth = Math.max(1, (int) Math.ceil(w * cos + h * sin)), newHeight = Math.max(1, (int) Math.ceil(w * sin + h * cos));
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB); Graphics2D g = rotated.createGraphics(); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g.translate(newWidth / 2.0, newHeight / 2.0); g.rotate(radians); g.drawImage(item.image, -w / 2, -h / 2, null); g.dispose();
        item.image = rotated; item.x = oldX + (w - newWidth) / 2; item.y = oldY + (h - newHeight) / 2; int newX = item.x, newY = item.y; BufferedImage after = copy(rotated);
        undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); item.image = copy(before); item.x = oldX; item.y = oldY; canvas.revalidate(); canvas.repaint(); } @Override public void redo() { super.redo(); item.image = copy(after); item.x = newX; item.y = newY; canvas.revalidate(); canvas.repaint(); } });
        canvas.revalidate(); canvas.repaint(); updateStatus("Layer rotated " + angle.getValue() + " degrees");
    }

    private void addItemImageEdit(MovableItem item, BufferedImage before, BufferedImage after) {
        undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); item.image = copy(before); canvas.revalidate(); canvas.repaint(); } @Override public void redo() { super.redo(); item.image = copy(after); canvas.revalidate(); canvas.repaint(); } });
        canvas.revalidate(); canvas.repaint();
    }

    private void applyShadow() {
        MovableItem item = layerList.getSelectedValue();
        if (item == null) { JOptionPane.showMessageDialog(this, "Select a text, image, or shape layer first.", "Shadow", JOptionPane.INFORMATION_MESSAGE); return; }
        BufferedImage before = copy(item.image); int oldX = item.x, oldY = item.y, offset = Math.max(4, brushSize / 2);
        BufferedImage result = new BufferedImage(item.image.getWidth() + offset, item.image.getHeight() + offset, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < item.image.getHeight(); y++) for (int x = 0; x < item.image.getWidth(); x++) { int alpha = (item.image.getRGB(x, y) >>> 24) & 255; int shadowAlpha = (int) (alpha * .45); if (shadowAlpha > 0) result.setRGB(x + offset, y + offset, shadowAlpha << 24); }
        Graphics2D g = result.createGraphics(); g.drawImage(item.image, 0, 0, null); g.dispose(); item.image = result; item.x -= offset / 2; item.y -= offset / 2;
        BufferedImage after = copy(item.image); int newX = item.x, newY = item.y;
        undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); item.image = copy(before); item.x = oldX; item.y = oldY; canvas.repaint(); } @Override public void redo() { super.redo(); item.image = copy(after); item.x = newX; item.y = newY; canvas.repaint(); } });
        canvas.revalidate(); canvas.repaint(); updateStatus("Shadow added");
    }

    private BufferedImage renderTextImage(String text, String family, int style, int size, Color color) {
        return renderStyledTextImage(text, family, style, size, color, new Color(0, 0, 0, 0), 0, false, new Color(0, 0, 0, 150), 4);
    }

    private BufferedImage renderStyledTextImage(String text, String family, int style, int size, Color color, Color borderColor, int borderWidth, boolean shadow, Color shadowColor, int shadowOffset) {
        Font font = new Font(family, style, size); String value = text == null ? "" : text;
        BufferedImage metricsImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); Graphics2D metricsGraphics = metricsImage.createGraphics(); metricsGraphics.setFont(font); FontMetrics metrics = metricsGraphics.getFontMetrics();
        int padding = Math.max(3, borderWidth + 2); if (shadow) padding = Math.max(padding, shadowOffset + 2);
        int width = Math.max(1, metrics.stringWidth(value) + padding * 2), height = Math.max(1, metrics.getHeight() + padding * 2); metricsGraphics.dispose();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); g.setFont(font); int baseline = padding + metrics.getAscent();
        GlyphVector glyphs = font.createGlyphVector(g.getFontRenderContext(), value); Shape textShape = glyphs.getOutline(padding, baseline);
        if (shadow) { g.setColor(shadowColor == null ? new Color(0, 0, 0, 150) : shadowColor); Shape shadowShape = AffineTransform.getTranslateInstance(shadowOffset, shadowOffset).createTransformedShape(textShape); g.fill(shadowShape); }
        if (borderWidth > 0 && borderColor != null && borderColor.getAlpha() > 0) { g.setColor(borderColor); g.setStroke(new BasicStroke(borderWidth * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(textShape); }
        g.setColor(color == null ? Color.BLACK : color); g.drawString(value, padding, baseline); g.dispose(); return result;
    }

    private void editTextLayer(MovableItem item) {
        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        JComboBox<String> family = new JComboBox<>(families); family.setSelectedItem(item.fontFamily == null ? Font.SANS_SERIF : item.fontFamily);
        JComboBox<String> style = new JComboBox<>(new String[]{"Regular", "Bold", "Italic", "Bold Italic"}); style.setSelectedIndex(item.fontStyle == Font.BOLD ? 1 : item.fontStyle == Font.ITALIC ? 2 : item.fontStyle == (Font.BOLD | Font.ITALIC) ? 3 : 0);
        JSpinner size = new JSpinner(new SpinnerNumberModel(item.fontSize > 0 ? item.fontSize : 24, 8, 300, 1));
        JButton textColor = new JButton("Choose"); textColor.setBackground(item.textColor == null ? Color.BLACK : item.textColor); textColor.setOpaque(true); textColor.setBorderPainted(true);
        JButton borderColor = new JButton("Choose"); borderColor.setBackground(item.borderColor == null ? new Color(0, 0, 0, 0) : item.borderColor); borderColor.setOpaque(true); borderColor.setBorderPainted(true);
        JButton shadowColor = new JButton("Choose"); shadowColor.setBackground(item.shadowColor == null ? new Color(0, 0, 0, 150) : item.shadowColor); shadowColor.setOpaque(true); shadowColor.setBorderPainted(true);
        final Color[] selectedTextColor = {item.textColor == null ? Color.BLACK : item.textColor}, selectedBorderColor = {item.borderColor == null ? new Color(0, 0, 0, 0) : item.borderColor}, selectedShadowColor = {item.shadowColor == null ? new Color(0, 0, 0, 150) : item.shadowColor};
        JSpinner borderWidth = new JSpinner(new SpinnerNumberModel(item.borderWidth, 0, 20, 1)); JCheckBox shadow = new JCheckBox("Enable text shadow", item.textShadow); JSpinner shadowOffset = new JSpinner(new SpinnerNumberModel(item.shadowOffset, 1, 30, 1));
        textColor.addActionListener(e -> { Color value = JColorChooser.showDialog(this, "Text color", selectedTextColor[0]); if (value != null) { selectedTextColor[0] = value; textColor.setBackground(value); } });
        borderColor.addActionListener(e -> { Color value = JColorChooser.showDialog(this, "Border color", selectedBorderColor[0]); if (value != null) { selectedBorderColor[0] = value; borderColor.setBackground(value); if ((Integer) borderWidth.getValue() == 0) borderWidth.setValue(1); } });
        shadowColor.addActionListener(e -> { Color value = JColorChooser.showDialog(this, "Shadow color", selectedShadowColor[0]); if (value != null) { selectedShadowColor[0] = value; shadowColor.setBackground(value); } });
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6)); form.add(new JLabel("Font")); form.add(family); form.add(new JLabel("Style")); form.add(style); form.add(new JLabel("Size")); form.add(size); form.add(new JLabel("Text color")); form.add(textColor); form.add(new JLabel("Border color")); form.add(borderColor); form.add(new JLabel("Border width")); form.add(borderWidth); form.add(shadow); form.add(new JLabel("")); form.add(new JLabel("Shadow color")); form.add(shadowColor); form.add(new JLabel("Shadow offset")); form.add(shadowOffset);
        if (JOptionPane.showConfirmDialog(this, form, "Edit Text", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        BufferedImage before = copy(item.image); String oldFamily = item.fontFamily; int oldStyle = item.fontStyle, oldSize = item.fontSize; Color oldTextColor = item.textColor, oldBorderColor = item.borderColor, oldShadowColor = item.shadowColor; int oldBorderWidth = item.borderWidth, oldShadowOffset = item.shadowOffset; boolean oldShadow = item.textShadow;
        item.fontFamily = (String) family.getSelectedItem(); item.fontStyle = new int[]{Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC}[style.getSelectedIndex()]; item.fontSize = (Integer) size.getValue();
        item.textColor = selectedTextColor[0]; item.borderColor = selectedBorderColor[0]; item.borderWidth = (Integer) borderWidth.getValue(); item.textShadow = shadow.isSelected(); item.shadowColor = selectedShadowColor[0]; item.shadowOffset = (Integer) shadowOffset.getValue();
        item.image = renderStyledTextImage(item.textContent, item.fontFamily, item.fontStyle, item.fontSize, item.textColor, item.borderColor, item.borderWidth, item.textShadow, item.shadowColor, item.shadowOffset);
        BufferedImage after = copy(item.image);
        undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); item.image = copy(before); item.fontFamily = oldFamily; item.fontStyle = oldStyle; item.fontSize = oldSize; item.textColor = oldTextColor; item.borderColor = oldBorderColor; item.borderWidth = oldBorderWidth; item.textShadow = oldShadow; item.shadowColor = oldShadowColor; item.shadowOffset = oldShadowOffset; canvas.repaint(); } @Override public void redo() { super.redo(); item.image = copy(after); item.fontFamily = (String) family.getSelectedItem(); item.fontStyle = new int[]{Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC}[style.getSelectedIndex()]; item.fontSize = (Integer) size.getValue(); item.textColor = selectedTextColor[0]; item.borderColor = selectedBorderColor[0]; item.borderWidth = (Integer) borderWidth.getValue(); item.textShadow = shadow.isSelected(); item.shadowColor = selectedShadowColor[0]; item.shadowOffset = (Integer) shadowOffset.getValue(); canvas.repaint(); } });
        layerList.repaint(); canvas.repaint();
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics(); g.drawImage(source, 0, 0, null); g.dispose(); return result;
    }

    private static BufferedImage createAppIcon() {
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(40, 105, 190), 64, 64, new Color(20, 45, 110))); g.fillRoundRect(2, 2, 60, 60, 14, 14);
        g.setColor(new Color(15, 35, 90, 150)); g.fillRoundRect(8, 8, 48, 48, 10, 10);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 31)); g.drawString("Ps", 13, 41);
        g.setColor(new Color(125, 205, 255)); g.setStroke(new BasicStroke(2)); g.drawLine(13, 48, 50, 48); g.dispose(); return icon;
    }

    private BufferedImage compositeImage() {
        BufferedImage result = copy(image); Graphics2D g = result.createGraphics();
        for (MovableItem item : movableItems) if (item.visible) { g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, item.opacity)); g.drawImage(item.image, item.x, item.y, null); }
        g.dispose(); return result;
    }

    private final class CanvasPanel extends JPanel {
        private int lastX, lastY;
        private boolean drawing;
        private boolean hasLastPoint;
        private double zoom = 1.0;
        private int cropStartX, cropStartY, cropEndX, cropEndY;
        private int shapeStartX, shapeStartY, shapeEndX, shapeEndY;
        private Rectangle selection;
        private int gradientStartX, gradientStartY, gradientEndX, gradientEndY;
        private MovableItem selectedItem;
        private int moveOffsetX, moveOffsetY, oldItemX, oldItemY;
        private MovableItem editingItem;
        private BufferedImage itemBeforeErase;
        private int itemLastX, itemLastY;
        private boolean itemStrokeHasPoint;
        private int pickerScreenX, pickerScreenY; private Point pickerPoint; private Color pickerColor = Color.BLACK; private boolean pickerHover;
        private final List<Point> perspectivePoints = new ArrayList<>();
        private MovableItem rotatingItem;
        private BufferedImage rotateBefore;
        private double rotateStartAngle, rotateCenterX, rotateCenterY;
        private int rotateOldX, rotateOldY;

        CanvasPanel() {
            setBackground(new Color(55, 55, 60));
            addMouseWheelListener(e -> {
                // The pointer location is the zoom anchor; no modifier key or slider is needed.
                double rotation = e.getPreciseWheelRotation();
                if (rotation == 0) return;
                double nextZoom = zoom * Math.pow(1.12, -rotation);
                zoomAt(nextZoom, e.getPoint());
                e.consume();
            });
            setTransferHandler(new TransferHandler() {
                @Override public boolean canImport(TransferSupport support) { return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor); }
                @Override public boolean importData(TransferSupport support) {
                    if (!canImport(support)) return false;
                    try {
                        @SuppressWarnings("unchecked") List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        Point drop = support.getDropLocation().getDropPoint();
                        Point target = imagePoint(drop.x, drop.y); boolean added = false;
                        for (File file : files) {
                            BufferedImage dropped = ImageIO.read(file);
                            if (dropped != null) { addOverlayImage(dropped, target.x, target.y, file.getName()); target.translate(20, 20); added = true; }
                        }
                        return added;
                    } catch (Exception ex) { showError(ex); return false; }
                }
            });
            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) { showContextMenu(e); return; }
                    if (tool == Tool.TEXT) { addTextAt(e.getX(), e.getY()); return; }
                    Point point = imagePoint(e.getX(), e.getY());
                    if (tool == Tool.CROP && perspectiveCropMode) { perspectivePoints.add(point); if (perspectivePoints.size() == 4) applyPerspectivePoints(); repaint(); return; }
                    if (tool == Tool.EYEDROPPER) { if (pickerHover && pickerPoint != null && isInsidePickerLens(e.getX(), e.getY())) point = pickerPoint; pickerScreenX = e.getX(); pickerScreenY = e.getY(); pickerPoint = point; pickerHover = true; pickColor(point.x, point.y); return; }
                    if (tool == Tool.FILL) { fillAt(point.x, point.y); return; }
                    if (tool == Tool.SELECT) { drawing = true; cropStartX = cropEndX = point.x; cropStartY = cropEndY = point.y; selection = new Rectangle(point.x, point.y, 0, 0); repaint(); return; }
                    if (tool == Tool.GRADIENT) { drawing = true; gradientStartX = gradientEndX = point.x; gradientStartY = gradientEndY = point.y; repaint(); return; }
                    if (tool == Tool.SHAPE) { drawing = true; shapeStartX = shapeEndX = point.x; shapeStartY = shapeEndY = point.y; repaint(); return; }
                    if (tool == Tool.ROTATE) {
                        MovableItem hit = findItem(point.x, point.y);
                        if (hit != null) { rotatingItem = hit; layerList.setSelectedValue(hit, true); rotateBefore = copy(hit.image); rotateOldX = hit.x; rotateOldY = hit.y; rotateCenterX = hit.x + hit.image.getWidth() / 2.0; rotateCenterY = hit.y + hit.image.getHeight() / 2.0; rotateStartAngle = Math.atan2(point.y - rotateCenterY, point.x - rotateCenterX); drawing = true; updateStatus("Drag around the layer to rotate it"); return; }
                        return;
                    }
                    if (tool == Tool.MOVE) {
                        MovableItem hit = findItem(point.x, point.y);
                        if (hit != null) { selectedItem = hit; layerList.setSelectedValue(hit, true); oldItemX = hit.x; oldItemY = hit.y; moveOffsetX = point.x - hit.x; moveOffsetY = point.y - hit.y; drawing = true; return; }
                        return;
                    }
                    if (tool == Tool.BRUSH || tool == Tool.ERASER) {
                        MovableItem hit = findItem(point.x, point.y);
                        if (hit != null) { editingItem = hit; layerList.setSelectedValue(hit, true); itemBeforeErase = copy(hit.image); itemLastX = point.x - hit.x; itemLastY = point.y - hit.y; itemStrokeHasPoint=false; drawing = true; editItemAt(point.x, point.y); return; }
                    }
                    drawing = true; hasLastPoint = false;
                    if (tool == Tool.CROP) { cropStartX = cropEndX = point.x; cropStartY = cropEndY = point.y; repaint(); }
                    else { beforeStroke = copy(image); drawAtScreen(e.getX(), e.getY()); }
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (!drawing) return;
                    if (editingItem != null) { Point point = imagePoint(e.getX(), e.getY()); editItemAt(point.x, point.y); return; }
                    if (rotatingItem != null) { Point point = imagePoint(e.getX(), e.getY()); double angle = Math.atan2(point.y - rotateCenterY, point.x - rotateCenterX) - rotateStartAngle; BufferedImage rotated = rotateImage(rotateBefore, angle); rotatingItem.image = rotated; rotatingItem.x = (int) Math.round(rotateCenterX - rotated.getWidth() / 2.0); rotatingItem.y = (int) Math.round(rotateCenterY - rotated.getHeight() / 2.0); repaint(); return; }
                    if (selectedItem != null) { Point point = imagePoint(e.getX(), e.getY()); selectedItem.x = Math.max(0, Math.min(image.getWidth() - selectedItem.image.getWidth(), point.x - moveOffsetX)); selectedItem.y = Math.max(0, Math.min(image.getHeight() - selectedItem.image.getHeight(), point.y - moveOffsetY)); repaint(); return; }
                    if (tool == Tool.MOVE) return;
                    if (tool == Tool.SELECT) { Point point = imagePoint(e.getX(), e.getY()); cropEndX = point.x; cropEndY = point.y; selection = new Rectangle(Math.min(cropStartX, cropEndX), Math.min(cropStartY, cropEndY), Math.abs(cropEndX - cropStartX), Math.abs(cropEndY - cropStartY)); repaint(); return; }
                    if (tool == Tool.GRADIENT) { Point point = imagePoint(e.getX(), e.getY()); gradientEndX = point.x; gradientEndY = point.y; repaint(); return; }
                    if (tool == Tool.SHAPE) { Point point = imagePoint(e.getX(), e.getY()); shapeEndX = point.x; shapeEndY = point.y; repaint(); return; }
                    if (tool == Tool.CROP) { setCropEnd(imagePoint(e.getX(), e.getY())); repaint(); }
                    else drawAtScreen(e.getX(), e.getY());
                }
                @Override public void mouseMoved(MouseEvent e) {
                    if (tool == Tool.EYEDROPPER) { pickerScreenX = e.getX(); pickerScreenY = e.getY(); pickerPoint = imagePoint(e.getX(), e.getY()); pickerColor = samplePickerColor(pickerPoint.x, pickerPoint.y); pickerHover = true; if (pickerValueLabel != null) pickerValueLabel.setText("RGB: " + pickerColor.getRed() + ", " + pickerColor.getGreen() + ", " + pickerColor.getBlue()); repaint(); }
                }
                @Override public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) return;
                    if (!drawing) return;
                    drawing = false;
                    if (editingItem != null) { MovableItem erased = editingItem; BufferedImage before = itemBeforeErase; BufferedImage after = copy(erased.image); editingItem = null; itemStrokeHasPoint=false; undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); erased.image = copy(before); repaint(); } @Override public void redo() { super.redo(); erased.image = copy(after); repaint(); } }); repaint(); return; }
                    if (rotatingItem != null) { MovableItem rotatedItem = rotatingItem; BufferedImage before = rotateBefore; BufferedImage after = copy(rotatedItem.image); int oldX = rotateOldX, oldY = rotateOldY, newX = rotatedItem.x, newY = rotatedItem.y; rotatingItem = null; rotateBefore = null; undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); rotatedItem.image = copy(before); rotatedItem.x = oldX; rotatedItem.y = oldY; revalidate(); repaint(); } @Override public void redo() { super.redo(); rotatedItem.image = copy(after); rotatedItem.x = newX; rotatedItem.y = newY; revalidate(); repaint(); } }); revalidate(); repaint(); return; }
                    if (selectedItem != null) { MovableItem moved = selectedItem; int newX = moved.x, newY = moved.y; selectedItem = null; undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); moved.x = oldItemX; moved.y = oldItemY; repaint(); } @Override public void redo() { super.redo(); moved.x = newX; moved.y = newY; repaint(); } }); repaint(); return; }
                    if (tool == Tool.SHAPE) { Point point = imagePoint(e.getX(), e.getY()); shapeEndX = point.x; shapeEndY = point.y; addShape(); return; }
                    if (tool == Tool.GRADIENT) { Point point = imagePoint(e.getX(), e.getY()); gradientEndX = point.x; gradientEndY = point.y; applyGradient(); return; }
                    if (tool == Tool.SELECT) { Point point = imagePoint(e.getX(), e.getY()); cropEndX = point.x; cropEndY = point.y; selection = new Rectangle(Math.min(cropStartX, cropEndX), Math.min(cropStartY, cropEndY), Math.abs(cropEndX - cropStartX), Math.abs(cropEndY - cropStartY)); drawing = false; repaint(); return; }
                    if (tool == Tool.CROP) { setCropEnd(imagePoint(e.getX(), e.getY())); cropSelection(); return; }
                    BufferedImage before = beforeStroke; BufferedImage after = copy(image); undo.addEdit(new AbstractUndoableEdit() {
                    @Override public void undo() { super.undo(); image = copy(before); repaint(); }
                    @Override public void redo() { super.redo(); image = copy(after); repaint(); }
                }); }
            };
            addMouseListener(mouse); addMouseMotionListener(mouse);
        }

        private void showContextMenu(MouseEvent event) {
            Point point = imagePoint(event.getX(), event.getY());
            MovableItem item = findItem(point.x, point.y);
            JPopupMenu menu = new JPopupMenu();
            if (tool == Tool.SELECT) {
                JMenuItem selectAll = new JMenuItem("Select All"); selectAll.addActionListener(e -> { selection = new Rectangle(0, 0, image.getWidth(), image.getHeight()); cropStartX = cropStartY = 0; cropEndX = image.getWidth(); cropEndY = image.getHeight(); repaint(); updateStatus("Entire canvas selected: " + image.getWidth() + " × " + image.getHeight() + " px"); });
                JMenuItem deselect = new JMenuItem("Deselect"); deselect.setEnabled(selection != null); deselect.addActionListener(e -> { selection = null; repaint(); updateStatus("Selection cleared"); });
                MovableItem cropTarget = item != null ? item : layerList.getSelectedValue();
                JMenuItem cropToSelection = new JMenuItem("Crop Selected Layer to Selection"); cropToSelection.setEnabled(selection != null && selection.width > 1 && selection.height > 1 && cropTarget != null); cropToSelection.addActionListener(e -> { if (selection == null || cropTarget == null) return; int sx=selection.x, sy=selection.y, sw=selection.width, sh=selection.height; Rectangle overlap=selection.intersection(new Rectangle(cropTarget.x,cropTarget.y,cropTarget.image.getWidth(),cropTarget.image.getHeight())); if(overlap.width<2||overlap.height<2)return; layerList.setSelectedValue(cropTarget,true); cropItemSelection(cropTarget,sx,sy,sw,sh); selection=null; repaint(); updateStatus("Layer cropped to selection; canvas unchanged"); });
                JMenuItem selectLayer = new JMenuItem(item == null ? "Select Layer Under Pointer" : "Select Layer: " + item.name); selectLayer.setEnabled(item != null); selectLayer.addActionListener(e -> { if (item != null) { layerList.setSelectedValue(item, true); selectedItem = item; updateLayerProperties(); repaint(); updateStatus("Selected " + item.name); } });
                menu.add(selectAll); menu.add(deselect); menu.add(cropToSelection); menu.addSeparator(); menu.add(selectLayer);
            } else if (item != null) {
                JMenuItem select = new JMenuItem("Select"); select.addActionListener(e -> { layerList.setSelectedValue(item, true); selectedItem = item; tool = Tool.MOVE; updateToolCursor(); updateLayerProperties(); updateStatus("Selected " + item.name); repaint(); });
                JMenuItem move = new JMenuItem("Move Object"); move.addActionListener(e -> { tool = Tool.MOVE; updateToolCursor(); updateStatus("Drag object to move it"); });
                JMenuItem erase = new JMenuItem("Erase Object Pixels"); erase.addActionListener(e -> { tool = Tool.ERASER; updateToolCursor(); updateStatus("Erase pixels from selected object"); });
                JMenuItem resize = new JMenuItem("Resize..."); resize.addActionListener(e -> resizeItem(item));
                JMenuItem freeRotate = new JMenuItem("Rotate Angle..."); freeRotate.addActionListener(e -> rotateItemFreely(item));
                JMenuItem rotate = new JMenuItem("Rotate 90°"); rotate.addActionListener(e -> transformItem(item, "rotate"));
                JMenuItem flipH = new JMenuItem("Flip Horizontal"); flipH.addActionListener(e -> transformItem(item, "flipH"));
                JMenuItem flipV = new JMenuItem("Flip Vertical"); flipV.addActionListener(e -> transformItem(item, "flipV"));
                JMenuItem solo = new JMenuItem("Show Only This Layer"); solo.addActionListener(e -> { for (MovableItem layer : movableItems) layer.visible = layer == item; refreshLayers(); repaint(); updateStatus("Showing only " + item.name); });
                JMenuItem front = new JMenuItem("Bring to Front"); front.addActionListener(e -> { movableItems.remove(item); movableItems.add(item); refreshLayers(); repaint(); });
                JMenuItem back = new JMenuItem("Send to Back"); back.addActionListener(e -> { movableItems.remove(item); movableItems.add(0, item); refreshLayers(); repaint(); });
                JMenuItem delete = new JMenuItem("Delete Object"); delete.addActionListener(e -> { unregisterItem(item); repaint(); updateStatus("Object deleted"); });
                menu.add(select); menu.add(move); menu.add(erase); menu.add(resize); menu.add(freeRotate); menu.add(rotate); menu.add(flipH); menu.add(flipV); menu.addSeparator(); menu.add(solo); menu.addSeparator(); menu.add(front); menu.add(back); menu.addSeparator(); menu.add(delete);
            } else {
                JMenuItem overlay = new JMenuItem("Place Image Overlay..."); overlay.addActionListener(e -> placeOverlay());
                JMenuItem text = new JMenuItem("Add Text Here"); text.addActionListener(e -> addTextAt(event.getX(), event.getY()));
                JMenuItem clear = new JMenuItem("Clear Canvas"); clear.addActionListener(e -> clearCanvas());
                menu.add(overlay); menu.add(text); menu.addSeparator(); menu.add(clear);
            }
            menu.addSeparator();
            JMenuItem undoItem = new JMenuItem("Undo"); undoItem.addActionListener(e -> { if (undo.canUndo()) undo.undo(); syncControlValues(); repaint(); });
            JMenuItem redoItem = new JMenuItem("Redo"); redoItem.addActionListener(e -> { if (undo.canRedo()) undo.redo(); syncControlValues(); repaint(); });
            menu.add(undoItem); menu.add(redoItem); menu.show(this, event.getX(), event.getY());
        }

        void setZoom(double zoom) { this.zoom = Math.max(0.1, Math.min(8.0, zoom)); if (zoomValueLabel != null) zoomValueLabel.setText(Math.round(this.zoom * 100) + "%"); revalidate(); repaint(); }
        private void zoomAt(double requestedZoom, Point canvasPoint) {
            double oldZoom = zoom;
            int oldImageX = Math.max(20, (getWidth() - (int)(image.getWidth() * oldZoom)) / 2);
            int oldImageY = Math.max(20, (getHeight() - (int)(image.getHeight() * oldZoom)) / 2);
            double imageX = (canvasPoint.x - oldImageX) / oldZoom, imageY = (canvasPoint.y - oldImageY) / oldZoom;
            JViewport viewport = canvasScroll.getViewport();
            Point anchorInViewport = SwingUtilities.convertPoint(this, canvasPoint, viewport);
            setZoom(requestedZoom);
            SwingUtilities.invokeLater(() -> {
                int imageXOnCanvas = Math.max(20, (getWidth() - (int)(image.getWidth() * zoom)) / 2) + (int)Math.round(imageX * zoom);
                int imageYOnCanvas = Math.max(20, (getHeight() - (int)(image.getHeight() * zoom)) / 2) + (int)Math.round(imageY * zoom);
                int maxX = Math.max(0, getWidth() - viewport.getWidth()), maxY = Math.max(0, getHeight() - viewport.getHeight());
                viewport.setViewPosition(new Point(Math.max(0, Math.min(maxX, imageXOnCanvas - anchorInViewport.x)), Math.max(0, Math.min(maxY, imageYOnCanvas - anchorInViewport.y))));
            });
        }
        double getZoom() { return zoom; }


        @Override public Dimension getPreferredSize() {
            return new Dimension((int) (image.getWidth() * zoom) + 40, (int) (image.getHeight() * zoom) + 40);
        }

        private void drawAtScreen(int screenX, int screenY) {
            int scaledWidth = (int) (image.getWidth() * zoom), scaledHeight = (int) (image.getHeight() * zoom);
            int offsetX = Math.max(20, (getWidth() - scaledWidth) / 2);
            int offsetY = Math.max(20, (getHeight() - scaledHeight) / 2);
            int x = (int) ((screenX - offsetX) / zoom), y = (int) ((screenY - offsetY) / zoom);
            if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
            if (hasLastPoint && x == lastX && y == lastY) return;
            if (!hasLastPoint) { lastX = x; lastY = y; hasLastPoint = true; }
            Graphics2D g = image.createGraphics();
            if (selection != null) g.clip(selection);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawStyledBrush(g,lastX,lastY,x,y,eraser);
            g.dispose(); lastX = x; lastY = y; repaint();
        }

        private Point imagePoint(int screenX, int screenY) {
            int scaledWidth = (int) (image.getWidth() * zoom), scaledHeight = (int) (image.getHeight() * zoom);
            int offsetX = Math.max(20, (getWidth() - scaledWidth) / 2), offsetY = Math.max(20, (getHeight() - scaledHeight) / 2);
            int x = Math.max(0, Math.min(image.getWidth() - 1, (int) ((screenX - offsetX) / zoom)));
            int y = Math.max(0, Math.min(image.getHeight() - 1, (int) ((screenY - offsetY) / zoom)));
            return new Point(x, y);
        }

        private MovableItem findItem(int x, int y) {
            for (int i = movableItems.size() - 1; i >= 0; i--) if (movableItems.get(i).visible && movableItems.get(i).contains(x, y)) return movableItems.get(i);
            return null;
        }

        private void setCropEnd(Point point) {
            if (cropAspect <= 0) { cropEndX = point.x; cropEndY = point.y; return; }
            int dx = point.x - cropStartX, dy = point.y - cropStartY, signX = dx < 0 ? -1 : 1, signY = dy < 0 ? -1 : 1;
            int width = Math.abs(dx), height = Math.abs(dy);
            if (width < 1 && height < 1) { cropEndX = point.x; cropEndY = point.y; return; }
            if (height <= 0 || (width / Math.max(1.0, (double) height)) >= cropAspect) height = Math.max(1, (int) Math.round(width / cropAspect));
            else width = Math.max(1, (int) Math.round(height * cropAspect));
            int endX = cropStartX + signX * width, endY = cropStartY + signY * height;
            endX = Math.max(0, Math.min(image.getWidth(), endX)); endY = Math.max(0, Math.min(image.getHeight(), endY));
            cropEndX = endX; cropEndY = endY;
        }

        private BufferedImage rotateImage(BufferedImage source, double radians) {
            int w = source.getWidth(), h = source.getHeight(); double sin = Math.abs(Math.sin(radians)), cos = Math.abs(Math.cos(radians));
            int newWidth = Math.max(1, (int) Math.ceil(w * cos + h * sin)), newHeight = Math.max(1, (int) Math.ceil(w * sin + h * cos));
            BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB); Graphics2D g = result.createGraphics(); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g.translate(newWidth / 2.0, newHeight / 2.0); g.rotate(radians); g.drawImage(source, -w / 2, -h / 2, null); g.dispose(); return result;
        }

        private MovableItem findOverlappingItem(int x, int y, int width, int height) {
            Rectangle cropArea = new Rectangle(x, y, width, height);
            for (int i = movableItems.size() - 1; i >= 0; i--) {
                MovableItem item = movableItems.get(i);
                if (item.visible && cropArea.intersects(new Rectangle(item.x, item.y, item.image.getWidth(), item.image.getHeight()))) return item;
            }
            return null;
        }

        private void editItemAt(int imageX, int imageY) {
            if (editingItem == null) return;
            int localX = imageX - editingItem.x, localY = imageY - editingItem.y;
            if(itemStrokeHasPoint&&localX==itemLastX&&localY==itemLastY)return;
            Graphics2D g = editingItem.image.createGraphics();
            if (selection != null) g.clip(new Rectangle(selection.x - editingItem.x, selection.y - editingItem.y, selection.width, selection.height));
            drawStyledBrush(g,itemLastX,itemLastY,localX,localY,eraser); g.dispose();
            itemLastX = localX; itemLastY = localY; itemStrokeHasPoint=true; repaint();
        }

        private void drawStyledBrush(Graphics2D base,int x1,int y1,int x2,int y2,boolean eraseStroke) {
            Graphics2D g=(Graphics2D)base.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(eraseStroke)g.setComposite(AlphaComposite.Clear);else g.setColor(brushColor);
            int radius=Math.max(1,brushSize/2);double distance=Math.hypot(x2-x1,y2-y1);int samples=Math.max(1,(int)Math.ceil(distance/Math.max(1,brushSize*.22)));
            switch(brushKind){
                case ROUND -> {g.setStroke(new BasicStroke(brushSize,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.drawLine(x1,y1,x2,y2);g.fillOval(x2-radius,y2-radius,brushSize,brushSize);}
                case SQUARE -> {g.setStroke(new BasicStroke(brushSize,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER));g.drawLine(x1,y1,x2,y2);g.fillRect(x2-radius,y2-radius,brushSize,brushSize);}
                case SOFT -> {if(!eraseStroke){Color c=brushColor;for(int i=0;i<=samples;i++){double t=i/(double)samples;float cx=(float)(x1+(x2-x1)*t),cy=(float)(y1+(y2-y1)*t);g.setPaint(new RadialGradientPaint(new Point2D.Float(cx,cy),Math.max(1f,brushSize/2f),new float[]{0,.52f,1},new Color[]{new Color(c.getRed(),c.getGreen(),c.getBlue(),110),new Color(c.getRed(),c.getGreen(),c.getBlue(),48),new Color(c.getRed(),c.getGreen(),c.getBlue(),0)}));g.fillOval((int)(cx-radius),(int)(cy-radius),brushSize,brushSize);}}else{g.setStroke(new BasicStroke(brushSize,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.drawLine(x1,y1,x2,y2);}}
                case MARKER -> {if(!eraseStroke)g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.34f));g.setStroke(new BasicStroke(brushSize,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_ROUND));g.drawLine(x1,y1,x2,y2);g.fillRect(x2-radius,y2-radius,brushSize,brushSize);}
                case SPRAY -> {java.util.Random random=new java.util.Random((((long)x1)<<32)^((long)y1<<16)^x2^((long)y2<<8)^brushSize);int dots=Math.max(18,(int)(distance*1.8)+brushSize);for(int i=0;i<dots;i++){double t=random.nextDouble(),angle=random.nextDouble()*Math.PI*2,rad=Math.sqrt(random.nextDouble())*radius;int px=(int)Math.round(x1+(x2-x1)*t+Math.cos(angle)*rad),py=(int)Math.round(y1+(y2-y1)*t+Math.sin(angle)*rad);int d=1+random.nextInt(Math.max(1,brushSize/9));if(!eraseStroke)g.setColor(new Color(brushColor.getRed(),brushColor.getGreen(),brushColor.getBlue(),145));g.fillOval(px-d/2,py-d/2,d,d);}}
                case CHALK -> {g.setStroke(new BasicStroke(brushSize,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.drawLine(x1,y1,x2,y2);g.fillOval(x2-radius,y2-radius,brushSize,brushSize);if(!eraseStroke){java.util.Random random=new java.util.Random((((long)x1)<<32)^y1^((long)x2<<8)^y2^brushSize);g.setColor(new Color(Math.min(255,brushColor.getRed()+45),Math.min(255,brushColor.getGreen()+45),Math.min(255,brushColor.getBlue()+45),115));for(int i=0;i<Math.max(12,brushSize);i++){double t=random.nextDouble(),angle=random.nextDouble()*Math.PI*2,rad=Math.sqrt(random.nextDouble())*radius;int px=(int)(x1+(x2-x1)*t+Math.cos(angle)*rad),py=(int)(y1+(y2-y1)*t+Math.sin(angle)*rad);g.fillOval(px,py,1+random.nextInt(2),1+random.nextInt(2));}}}
                case CALLIGRAPHY -> {double angle=-Math.PI/4;for(int i=0;i<=samples;i++){double t=i/(double)samples;Graphics2D dab=(Graphics2D)g.create();dab.translate(x1+(x2-x1)*t,y1+(y2-y1)*t);dab.rotate(angle);dab.fillRoundRect(-Math.max(1,brushSize/5),-radius,Math.max(2,brushSize/2),brushSize,Math.max(1,brushSize/5),Math.max(1,brushSize/5));dab.dispose();}}
                case DOTTED -> {int diameter=Math.max(2,(int)(brushSize*.62)),gap=Math.max(diameter+2,(int)(brushSize*1.55));int count=Math.max(1,(int)Math.ceil(distance/gap));for(int i=0;i<=count;i++){double t=count==0?0:i/(double)count;int px=(int)Math.round(x1+(x2-x1)*t),py=(int)Math.round(y1+(y2-y1)*t);g.fillOval(px-diameter/2,py-diameter/2,diameter,diameter);}}
            }
            g.dispose();
        }

        private void addTextAt(int screenX, int screenY) {
            String text = JOptionPane.showInputDialog(PhotoshopStudio.this, "Enter text:");
            if (text == null || text.isBlank()) return;
            Point point = imagePoint(screenX, screenY); int fontSize = Math.max(14, brushSize * 2);
            MovableItem item = new MovableItem(renderTextImage(text, Font.SANS_SERIF, Font.PLAIN, fontSize, brushColor), point.x, Math.max(0, point.y - fontSize), "Text: " + text);
            item.textContent = text; item.fontFamily = Font.SANS_SERIF; item.fontStyle = Font.PLAIN; item.fontSize = fontSize; item.textColor = brushColor; registerItem(item);
            undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); unregisterItem(item); repaint(); } @Override public void redo() { super.redo(); registerItem(item); repaint(); } });
            repaint();
        }

        private Color samplePickerColor(int x, int y) { if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return Color.BLACK; return new Color(compositeImage().getRGB(x, y), true); }
        private boolean isInsidePickerLens(int screenX, int screenY) { int lensSize = 130, lensX = pickerScreenX + 18, lensY = pickerScreenY + 18; if (lensX + lensSize > getWidth()) lensX = pickerScreenX - lensSize - 18; if (lensY + lensSize > getHeight()) lensY = pickerScreenY - lensSize - 18; double dx = screenX - (lensX + lensSize / 2.0), dy = screenY - (lensY + lensSize / 2.0); return dx * dx + dy * dy <= (lensSize / 2.0) * (lensSize / 2.0); }
        private void hidePickerLens() { pickerHover = false; repaint(); }
        private void clearPerspectivePoints() { perspectivePoints.clear(); repaint(); }
        private void applyPerspectivePoints() { Point a = perspectivePoints.get(0), b = perspectivePoints.get(1), c = perspectivePoints.get(2), d = perspectivePoints.get(3); int outputWidth = Math.max(2, (int) Math.round((a.distance(b) + d.distance(c)) / 2.0)), outputHeight = Math.max(2, (int) Math.round((a.distance(d) + b.distance(c)) / 2.0)); MovableItem selected = layerList.getSelectedValue(); if (selected != null) { BufferedImage before = copy(selected.image); int oldX = selected.x, oldY = selected.y; double[][] localCorners = {{a.x - selected.x, a.y - selected.y}, {b.x - selected.x, b.y - selected.y}, {c.x - selected.x, c.y - selected.y}, {d.x - selected.x, d.y - selected.y}}; BufferedImage result = perspectiveWarp(selected.image, localCorners, outputWidth, outputHeight); selected.image = result; selected.x = Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)); selected.y = Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)); int newX = selected.x, newY = selected.y; perspectivePoints.clear(); undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); selected.image = copy(before); selected.x = oldX; selected.y = oldY; refreshLayers(); repaint(); } @Override public void redo() { super.redo(); selected.image = copy(result); selected.x = newX; selected.y = newY; refreshLayers(); repaint(); } }); refreshLayers(); revalidate(); repaint(); updateStatus("Perspective crop applied to " + selected.name); return; } BufferedImage before = copy(image), result = perspectiveWarp(image, new double[][]{{a.x, a.y}, {b.x, b.y}, {c.x, c.y}, {d.x, d.y}}, outputWidth, outputHeight); image = result; perspectivePoints.clear(); addImageEdit(before, copy(result)); revalidate(); repaint(); updateStatus("Perspective crop applied"); }
        private void pickColor(int x, int y) { if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) { Color sampledColor = samplePickerColor(x, y); int redValue = sampledColor.getRed(), greenValue = sampledColor.getGreen(), blueValue = sampledColor.getBlue(); if (redSlider != null) redSlider.setValue(redValue); if (greenSlider != null) greenSlider.setValue(greenValue); if (blueSlider != null) blueSlider.setValue(blueValue); brushColor = new Color(redValue, greenValue, blueValue); pickerColor = brushColor; pickerPoint = new Point(x, y); pickerHover = true; if (colorPreviewPanel != null) colorPreviewPanel.setBackground(brushColor); if (colorButton != null) colorButton.setBackground(brushColor); if (pickerValueLabel != null) pickerValueLabel.setText("RGB: " + redValue + ", " + greenValue + ", " + blueValue); updateStatus("Picked RGB " + redValue + ", " + greenValue + ", " + blueValue); repaint(); } }

        private void fillAt(int x, int y) {
            if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
            if (selection != null && !selection.contains(x, y)) return;
            MovableItem selected = layerList.getSelectedValue(); BufferedImage targetImage = selected != null && selected.contains(x, y) ? selected.image : image; int ox = selected != null && targetImage == selected.image ? selected.x : 0, oy = selected != null && targetImage == selected.image ? selected.y : 0; int localX = x - ox, localY = y - oy;
            BufferedImage before = copy(targetImage); int target = targetImage.getRGB(localX, localY), replacement = brushColor.getRGB(); if (target == replacement) return;
            java.util.ArrayDeque<Point> queue = new java.util.ArrayDeque<>(); queue.add(new Point(localX, localY)); boolean[][] seen = new boolean[targetImage.getHeight()][targetImage.getWidth()];
            while (!queue.isEmpty()) { Point p = queue.remove(); int canvasX = p.x + ox, canvasY = p.y + oy; if (p.x < 0 || p.y < 0 || p.x >= targetImage.getWidth() || p.y >= targetImage.getHeight() || (selection != null && !selection.contains(canvasX, canvasY)) || seen[p.y][p.x] || targetImage.getRGB(p.x, p.y) != target) continue; seen[p.y][p.x] = true; targetImage.setRGB(p.x, p.y, replacement); queue.add(new Point(p.x + 1, p.y)); queue.add(new Point(p.x - 1, p.y)); queue.add(new Point(p.x, p.y + 1)); queue.add(new Point(p.x, p.y - 1)); }
            if (targetImage == image) addImageEdit(before, copy(image)); else addItemImageEdit(selected, before, copy(selected.image)); repaint();
        }

        private void applyGradient() {
            BufferedImage before = copy(image); Graphics2D g = image.createGraphics();
            if (selection != null && selection.width > 1 && selection.height > 1) g.clip(selection);
            g.setPaint(new GradientPaint(gradientStartX, gradientStartY, brushColor, gradientEndX, gradientEndY, new Color(brushColor.getRed(), brushColor.getGreen(), brushColor.getBlue(), 0), true)); g.fillRect(0, 0, image.getWidth(), image.getHeight()); g.dispose();
            addImageEdit(before, copy(image)); selection = null; drawing = false; repaint();
        }

        private void addShape() {
            int x = Math.min(shapeStartX, shapeEndX), y = Math.min(shapeStartY, shapeEndY), w = Math.abs(shapeEndX - shapeStartX), h = Math.abs(shapeEndY - shapeStartY);
            if (w < 2 || h < 2) return;
            int pad = Math.max(brushSize, 4); BufferedImage shapeImage = new BufferedImage(w + pad * 2, h + pad * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = shapeImage.createGraphics(); g.setColor(brushColor); g.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawShape(g, shapeKind, pad, pad, w, h); g.dispose();
            MovableItem item = new MovableItem(shapeImage, x - pad, y - pad, shapeKind.name().toLowerCase()); registerItem(item);
            undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); unregisterItem(item); repaint(); } @Override public void redo() { super.redo(); registerItem(item); repaint(); } });
            drawing = false; repaint();
        }

        private Polygon polygon(ShapeKind kind, int x, int y, int w, int h) {
            if(kind==ShapeKind.TRAPEZOID)return new Polygon(new int[]{x+w/4,x+3*w/4,x+w,x},new int[]{y,y,y+h,y+h},4);
            if(kind==ShapeKind.PARALLELOGRAM)return new Polygon(new int[]{x+w/4,x+w,x+3*w/4,x},new int[]{y,y,y+h,y+h},4);
            if(kind==ShapeKind.CHEVRON)return new Polygon(new int[]{x,x+w/2,x+w,x+3*w/4,x+w/2,x+w/4},new int[]{y,y+h/2,y,y+h,y+h/2,y+h},6);
            if(kind==ShapeKind.LIGHTNING)return new Polygon(new int[]{x+w/2,x+w/4,x+w/2,x+w/3,x+3*w/4,x+w/2},new int[]{y,y+h/2,y+h/2,y+h,x+2*h/5,y+2*h/5},6);
            int sides = kind == ShapeKind.TRIANGLE ? 3 : kind == ShapeKind.PENTAGON ? 5 : kind == ShapeKind.HEXAGON ? 6 : kind == ShapeKind.OCTAGON ? 8 : kind == ShapeKind.SIX_POINT_STAR ? 12 : 10;
            Polygon result = new Polygon(); double cx = x + w / 2.0, cy = y + h / 2.0, rx = w / 2.0, ry = h / 2.0;
            for (int i = 0; i < sides; i++) { double angle = -Math.PI / 2 + i * Math.PI * 2 / sides; double radius = (kind == ShapeKind.STAR || kind == ShapeKind.SIX_POINT_STAR) && i % 2 == 1 ? .45 : 1; result.addPoint((int) (cx + Math.cos(angle) * rx * radius), (int) (cy + Math.sin(angle) * ry * radius)); }
            return result;
        }

        private void drawShape(Graphics2D g, ShapeKind kind, int x, int y, int w, int h) {
            switch(kind){
                case RECTANGLE -> g.drawRect(x,y,w,h);
                case ROUND_RECTANGLE -> g.drawRoundRect(x,y,w,h,Math.min(w,h)/3,Math.min(w,h)/3);
                case ELLIPSE -> g.drawOval(x,y,w,h);
                case LINE -> g.drawLine(x,y,x+w,y+h);
                case ARROW -> {g.drawLine(x,y+h,x+w,y);g.drawLine(x+w,y,x+w-Math.max(8,w/5),y);g.drawLine(x+w,y,x+w,y+Math.max(8,h/5));}
                case CROSS -> {g.drawLine(x,y+h/2,x+w,y+h/2);g.drawLine(x+w/2,y,x+w/2,y+h);}
                case DIAMOND -> g.drawPolygon(new Polygon(new int[]{x+w/2,x+w,x+w/2,x},new int[]{y,y+h/2,y+h,y+h/2},4));
                case HEART -> {Path2D p=new Path2D.Double();p.moveTo(x+w/2.0,y+h);p.curveTo(x-w*.1,y+h*.55,x+w*.05,y+h*.05,x+w*.32,y+h*.18);p.curveTo(x+w*.43,y+h*.22,x+w*.48,y+h*.32,x+w/2.0,y+h*.38);p.curveTo(x+w*.52,y+h*.32,x+w*.57,y+h*.22,x+w*.68,y+h*.18);p.curveTo(x+w*.95,y+h*.05,x+w*1.1,y+h*.55,x+w/2.0,y+h);g.draw(p);}
                case CLOUD -> {Path2D p=new Path2D.Double();p.moveTo(x+w*.18,y+h*.75);p.curveTo(x-w*.02,y+h*.72,x+w*.02,y+h*.42,x+w*.25,y+h*.4);p.curveTo(x+w*.28,y+h*.08,x+w*.65,y+h*.08,x+w*.72,y+h*.38);p.curveTo(x+w*1.04,y+h*.34,x+w*1.02,y+h*.75,x+w*.8,y+h*.78);p.closePath();g.draw(p);}
                case PLUS -> {Path2D p=new Path2D.Double();p.moveTo(x+w*.4,y);p.lineTo(x+w*.6,y);p.lineTo(x+w*.6,y+h*.4);p.lineTo(x+w,y+h*.4);p.lineTo(x+w,y+h*.6);p.lineTo(x+w*.6,y+h*.6);p.lineTo(x+w*.6,y+h);p.lineTo(x+w*.4,y+h);p.lineTo(x+w*.4,y+h*.6);p.lineTo(x,y+h*.6);p.lineTo(x,y+h*.4);p.lineTo(x+w*.4,y+h*.4);p.closePath();g.draw(p);}
                case SPEECH_BUBBLE -> {Path2D p=new Path2D.Double();p.moveTo(x,y);p.lineTo(x+w,y);p.lineTo(x+w,y+h*.72);p.lineTo(x+w*.38,y+h*.72);p.lineTo(x+w*.2,y+h);p.lineTo(x+w*.22,y+h*.72);p.lineTo(x,y+h*.72);p.closePath();g.draw(p);}
                case CYLINDER -> {g.drawOval(x,y,w,h/3);g.drawLine(x,y+h/6,x,y+h*5/6);g.drawLine(x+w,y+h/6,x+w,y+h*5/6);g.drawArc(x,y+h*2/3,w,h/3,180,180);}
                case ARC -> g.drawArc(x,y,w,h,25,300);
                case PIE -> g.drawArc(x,y,w,h,30,300);
                default -> g.drawPolygon(polygon(kind,x,y,w,h));
            }
        }

        private void cropSelection() {
            int x = Math.min(cropStartX, cropEndX), y = Math.min(cropStartY, cropEndY);
            int w = Math.abs(cropEndX - cropStartX), h = Math.abs(cropEndY - cropStartY);
            if (w < 2 || h < 2) return;
            MovableItem selected = layerList.getSelectedValue();
            boolean overlapsSelected = selected != null && selected.visible && x < selected.x + selected.image.getWidth() && x + w > selected.x && y < selected.y + selected.image.getHeight() && y + h > selected.y;
            if (!overlapsSelected) selected = findOverlappingItem(x, y, w, h);
            if (selected != null) { layerList.setSelectedValue(selected, true); cropItemSelection(selected, x, y, w, h); return; }
            BufferedImage before = copy(image), cropped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cropped.createGraphics(); g.drawImage(image, 0, 0, w, h, x, y, x + w, y + h, null); g.dispose();
            if (circleCrop) cropped = circleMask(cropped); else if (scrapbookCrop) cropped = scrapbookMask(cropped);
            image = cropped; addImageEdit(before, copy(image)); cropStartX = cropStartY = cropEndX = cropEndY = 0; revalidate(); repaint();
        }

        private void cropItemSelection(MovableItem item, int x, int y, int w, int h) {
            int left = Math.max(x, item.x), top = Math.max(y, item.y), right = Math.min(x + w, item.x + item.image.getWidth()), bottom = Math.min(y + h, item.y + item.image.getHeight());
            if (right - left < 2 || bottom - top < 2) return;
            BufferedImage before = copy(item.image); int oldX = item.x, oldY = item.y;
            BufferedImage cropped = new BufferedImage(right - left, bottom - top, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cropped.createGraphics(); g.drawImage(item.image, 0, 0, cropped.getWidth(), cropped.getHeight(), left - item.x, top - item.y, right - item.x, bottom - item.y, null); g.dispose();
            if (circleCrop) cropped = circleMask(cropped); else if (scrapbookCrop) cropped = scrapbookMask(cropped);
            final BufferedImage croppedResult = cropped; item.image = croppedResult; item.x = left; item.y = top;
            undo.addEdit(new AbstractUndoableEdit() { @Override public void undo() { super.undo(); item.image = copy(before); item.x = oldX; item.y = oldY; repaint(); } @Override public void redo() { super.redo(); item.image = copy(croppedResult); item.x = left; item.y = top; repaint(); } });
            cropStartX = cropStartY = cropEndX = cropEndY = 0; refreshLayers(); repaint();
        }

        private BufferedImage circleMask(BufferedImage source) {
            BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB); Graphics2D g = result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g.setClip(new java.awt.geom.Ellipse2D.Double(0, 0, source.getWidth(), source.getHeight())); g.drawImage(source, 0, 0, null); g.dispose(); return result;
        }

        private BufferedImage scrapbookMask(BufferedImage source) {
            BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB); Graphics2D g = result.createGraphics();
            Path2D edge = new Path2D.Double(); int w = source.getWidth(), h = source.getHeight(), step = Math.max(8, Math.min(w, h) / 12); edge.moveTo(0, 0);
            for (int px = 0; px <= w; px += step) edge.lineTo(px, Math.max(0, 3 + (int) (Math.sin(px * 1.7) * 3)));
            for (int py = 0; py <= h; py += step) edge.lineTo(Math.min(w, w - 3 + (int) (Math.sin(py * 1.4) * 3)), py);
            for (int px = w; px >= 0; px -= step) edge.lineTo(px, Math.min(h, h - 3 + (int) (Math.sin(px * 1.9) * 3)));
            for (int py = h; py >= 0; py -= step) edge.lineTo(Math.max(0, 3 + (int) (Math.sin(py * 1.6) * 3)), py);
            edge.closePath(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g.setClip(edge); g.drawImage(source, 0, 0, null); g.dispose(); return result;
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            updateHistoryButtons();
            int width = (int) (image.getWidth() * zoom), height = (int) (image.getHeight() * zoom);
            int x = Math.max(20, (getWidth() - width) / 2), y = Math.max(20, (getHeight() - height) / 2);
            drawRulers((Graphics2D) graphics, x, y, width, height);
            graphics.drawImage(image, x, y, width, height, null);
            Graphics2D layerGraphics = (Graphics2D) graphics.create();
            for (MovableItem item : movableItems) if (item.visible) { layerGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, item.opacity)); layerGraphics.drawImage(item.image, x + (int) (item.x * zoom), y + (int) (item.y * zoom), (int) (item.image.getWidth() * zoom), (int) (item.image.getHeight() * zoom), null); }
            layerGraphics.dispose();
            if (tool == Tool.EYEDROPPER && pickerHover && pickerPoint != null) {
                int lensSize = 130, lensX = pickerScreenX + 18, lensY = pickerScreenY + 18, sample = Math.max(8, (int) (lensSize / Math.max(1.0, zoom * 4)));
                if (lensX + lensSize > getWidth()) lensX = pickerScreenX - lensSize - 18; if (lensY + lensSize > getHeight()) lensY = pickerScreenY - lensSize - 18;
                BufferedImage preview = compositeImage(); int left = Math.max(0, Math.min(preview.getWidth() - sample, pickerPoint.x - sample / 2)), top = Math.max(0, Math.min(preview.getHeight() - sample, pickerPoint.y - sample / 2));
                Graphics2D lens = (Graphics2D) graphics.create(); lens.setClip(new java.awt.geom.Ellipse2D.Double(lensX, lensY, lensSize, lensSize)); lens.drawImage(preview, lensX, lensY, lensX + lensSize, lensY + lensSize, left, top, left + sample, top + sample, null); lens.setClip(null); lens.setColor(Color.WHITE); lens.setStroke(new BasicStroke(3)); lens.drawOval(lensX, lensY, lensSize, lensSize); lens.setColor(Color.BLACK); lens.setStroke(new BasicStroke(1)); lens.drawLine(lensX + lensSize / 2, lensY + lensSize / 2 - 10, lensX + lensSize / 2, lensY + lensSize / 2 + 10); lens.drawLine(lensX + lensSize / 2 - 10, lensY + lensSize / 2, lensX + lensSize / 2 + 10, lensY + lensSize / 2); lens.setColor(pickerColor); lens.fillRect(lensX + 8, lensY + lensSize - 28, 22, 20); lens.setColor(Color.WHITE); lens.drawRect(lensX + 8, lensY + lensSize - 28, 22, 20); lens.setFont(lens.getFont().deriveFont(Font.BOLD, 12f)); lens.drawString("RGB " + pickerColor.getRed() + "," + pickerColor.getGreen() + "," + pickerColor.getBlue(), lensX + 36, lensY + lensSize - 14); lens.dispose();
            }
            if (tool == Tool.CROP && perspectiveCropMode && !perspectivePoints.isEmpty()) { Graphics2D guide = (Graphics2D) graphics.create(); guide.setColor(new Color(255, 190, 35)); guide.setStroke(new BasicStroke(2)); for (int i = 0; i < perspectivePoints.size(); i++) { Point current = perspectivePoints.get(i); int px = x + (int) (current.x * zoom), py = y + (int) (current.y * zoom); guide.fillOval(px - 6, py - 6, 12, 12); if (i > 0) { Point previous = perspectivePoints.get(i - 1); guide.drawLine(x + (int) (previous.x * zoom), y + (int) (previous.y * zoom), px, py); } } guide.dispose(); }
            if (tool == Tool.CROP && drawing) {
                int sx = x + (int) (Math.min(cropStartX, cropEndX) * zoom), sy = y + (int) (Math.min(cropStartY, cropEndY) * zoom);
                int sw = (int) (Math.abs(cropEndX - cropStartX) * zoom), sh = (int) (Math.abs(cropEndY - cropStartY) * zoom);
                Graphics2D g = (Graphics2D) graphics.create(); g.setColor(new Color(0, 0, 0, 105));
                g.fillRect(x, y, width, Math.max(0, sy - y)); g.fillRect(x, sy + sh, width, Math.max(0, y + height - sy - sh));
                g.fillRect(x, sy, Math.max(0, sx - x), sh); g.fillRect(sx + sw, sy, Math.max(0, x + width - sx - sw), sh);
                g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{8, 5}, 0)); if (circleCrop) g.drawOval(sx, sy, sw, sh); else g.drawRect(sx, sy, sw, sh);
                g.setColor(new Color(255, 210, 40)); g.setStroke(new BasicStroke(2)); if (circleCrop) g.drawOval(sx, sy, sw, sh); else g.drawRect(sx, sy, sw, sh);
                int handle = 8; g.fillRect(sx - handle / 2, sy - handle / 2, handle, handle); g.fillRect(sx + sw - handle / 2, sy - handle / 2, handle, handle); g.fillRect(sx - handle / 2, sy + sh - handle / 2, handle, handle); g.fillRect(sx + sw - handle / 2, sy + sh - handle / 2, handle, handle);
                String dimensions = Math.abs(cropEndX - cropStartX) + " × " + Math.abs(cropEndY - cropStartY) + " px"; g.setFont(g.getFont().deriveFont(Font.BOLD, 12f)); FontMetrics fm = g.getFontMetrics(); int labelWidth = fm.stringWidth(dimensions) + 12, labelX = Math.max(x, Math.min(sx, x + width - labelWidth)); int labelY = sy > y + 24 ? sy - 8 : Math.min(y + height - 4, sy + sh + 18); g.setColor(new Color(35, 35, 40, 220)); g.fillRoundRect(labelX, labelY - fm.getAscent(), labelWidth, fm.getHeight(), 6, 6); g.setColor(Color.WHITE); g.drawString(dimensions, labelX + 6, labelY); g.dispose();
            }
            if (selection != null && selection.width > 0 && selection.height > 0) { Graphics2D g = (Graphics2D) graphics.create(); int sx=x+(int)(selection.x*zoom), sy=y+(int)(selection.y*zoom), sw=(int)(selection.width*zoom), sh=(int)(selection.height*zoom); g.setColor(new Color(0,0,0,210)); g.setStroke(new BasicStroke(3)); g.drawRect(sx,sy,sw,sh); g.setColor(Color.WHITE); g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5,4}, 0)); g.drawRect(sx,sy,sw,sh); g.dispose(); }
            if (tool == Tool.SHAPE && drawing) {
                int sx = x + (int) (Math.min(shapeStartX, shapeEndX) * zoom), sy = y + (int) (Math.min(shapeStartY, shapeEndY) * zoom);
                int sw = (int) (Math.abs(shapeEndX - shapeStartX) * zoom), sh = (int) (Math.abs(shapeEndY - shapeStartY) * zoom);
                Graphics2D g = (Graphics2D) graphics.create(); g.setColor(brushColor); g.setStroke(new BasicStroke(Math.max(1, (int) (brushSize * zoom))));
                drawShape(g, shapeKind, sx, sy, sw, sh); g.dispose();
            }
        }

        private void drawRulers(Graphics2D graphics, int imageX, int imageY, int imageWidth, int imageHeight) {
            Graphics2D ruler = (Graphics2D) graphics.create(); Color background = new Color(232, 234, 239), tick = new Color(100, 105, 115); ruler.setColor(background); ruler.fillRect(imageX, imageY - 20, imageWidth, 20); ruler.fillRect(imageX - 20, imageY, 20, imageHeight); ruler.setColor(new Color(185, 190, 198)); ruler.drawLine(imageX, imageY - 1, imageX + imageWidth, imageY - 1); ruler.drawLine(imageX - 1, imageY, imageX - 1, imageY + imageHeight);
            ruler.setColor(tick); ruler.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9)); int step = 50; for (int value = 0; value <= image.getWidth(); value += step) { int px = imageX + (int) (value * zoom); ruler.drawLine(px, imageY - 7, px, imageY); if (value % 100 == 0) ruler.drawString(String.valueOf(value), px + 2, imageY - 9); } for (int value = 0; value <= image.getHeight(); value += step) { int py = imageY + (int) (value * zoom); ruler.drawLine(imageX - 7, py, imageX, py); if (value % 100 == 0) ruler.drawString(String.valueOf(value), imageX - 19, py - 2); } ruler.dispose();
        }
    }

    private static final class ControlIcon implements Icon {
        private final String type;
        ControlIcon(String type) { this.type = type; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (type.equals("red")) { g.setColor(new Color(220, 55, 60)); g.fillOval(x+3,y+3,10,10); g.setColor(new Color(125,30,35)); g.drawOval(x+3,y+3,10,10); }
            else if (type.equals("green")) { g.setColor(new Color(45,165,85)); g.fillOval(x+3,y+3,10,10); g.setColor(new Color(20,100,50)); g.drawOval(x+3,y+3,10,10); }
            else if (type.equals("blue")) { g.setColor(new Color(55,115,220)); g.fillOval(x+3,y+3,10,10); g.setColor(new Color(30,65,135)); g.drawOval(x+3,y+3,10,10); }
            else if (type.equals("brightness")) { g.setColor(new Color(225,160,35)); g.setStroke(new BasicStroke(1.5f)); g.drawOval(x+4,y+4,8,8); g.drawLine(x+8,y,x+8,y+3); g.drawLine(x+8,y+13,x+8,y+16); g.drawLine(x,y+8,x+3,y+8); g.drawLine(x+13,y+8,x+16,y+8); g.drawLine(x+2,y+2,x+4,y+4); g.drawLine(x+12,y+12,x+14,y+14); g.drawLine(x+12,y+4,x+14,y+2); g.drawLine(x+2,y+14,x+4,y+12); g.fillOval(x+6,y+6,4,4); }
            else if (type.equals("contrast")) { g.setColor(Color.WHITE); g.fillOval(x+2,y+2,12,12); g.setColor(new Color(65,75,95)); g.fillArc(x+2,y+2,12,12,90,180); g.setColor(new Color(55,60,70)); g.drawOval(x+2,y+2,12,12); }
            else if (type.equals("saturation")) { g.setColor(new Color(225,65,70)); g.fillOval(x+1,y+5,8,8); g.setColor(new Color(45,165,90)); g.fillOval(x+5,y+2,8,8); g.setColor(new Color(55,115,220)); g.fillOval(x+8,y+6,8,8); }
            g.dispose();
        }
    }

    private String shapeKindLabel(ShapeKind kind) { String words=kind.name().toLowerCase().replace('_',' '); StringBuilder label=new StringBuilder(); for(String word:words.split(" ")){if(label.length()>0)label.append(' ');label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));}return label.toString(); }
    private String brushKindLabel(BrushKind kind) { String words=kind.name().toLowerCase();return Character.toUpperCase(words.charAt(0))+words.substring(1); }

    private final class ShapeListRenderer extends JLabel implements ListCellRenderer<ShapeKind> {
        ShapeListRenderer(){setHorizontalAlignment(SwingConstants.CENTER);setOpaque(true);setBorder(BorderFactory.createEmptyBorder(2,2,2,2));}
        @Override public Component getListCellRendererComponent(JList<? extends ShapeKind> list,ShapeKind value,int index,boolean selected,boolean focus){setIcon(value==null?null:new ShapePreviewIcon(value));setText(value==null||index<0?"":shapeKindLabel(value));setHorizontalAlignment(index<0?SwingConstants.CENTER:SwingConstants.LEFT);setToolTipText(value==null?"Shape":shapeKindLabel(value));setBackground(selected?new Color(70,105,155):list.getBackground());setForeground(list.getForeground());setPreferredSize(new Dimension(index<0?34:170,29));return this;}
    }

    private final class ShapePreviewIcon implements Icon {
        private final ShapeKind kind; ShapePreviewIcon(ShapeKind kind){this.kind=kind;}
        @Override public int getIconWidth(){return 25;} @Override public int getIconHeight(){return 23;}
        @Override public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(55,120,205));g.setStroke(new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));canvas.drawShape(g,kind,x+3,y+3,19,17);g.dispose();}
    }

    private static final class BrushPreviewIcon implements Icon {
        private final BrushKind kind; BrushPreviewIcon(BrushKind kind){this.kind=kind;}
        @Override public int getIconWidth(){return 25;} @Override public int getIconHeight(){return 18;}
        @Override public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(45,110,195));g.setStroke(new BasicStroke(kind==BrushKind.CALLIGRAPHY?4:3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));switch(kind){case DOTTED,SPRAY->{for(int i=0;i<5;i++)g.fillOval(x+3+i*4,y+7+(i%2)*2,2,2);}case SQUARE->g.drawLine(x+3,y+8,x+21,y+8);case CALLIGRAPHY->{g.rotate(-Math.PI/4,x+12,y+9);g.drawLine(x+5,y+9,x+19,y+9);}case SOFT->{g.setColor(new Color(45,110,195,70));g.fillOval(x+4,y+2,17,13);g.setColor(new Color(45,110,195,180));g.fillOval(x+8,y+5,9,7);}case MARKER->{g.setStroke(new BasicStroke(5,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER));g.drawLine(x+4,y+9,x+20,y+9);}default->g.drawLine(x+3,y+9,x+21,y+9);}g.dispose();}
    }

    private static final class EditorIcon implements Icon {
        private final String type;
        EditorIcon(String type) { this.type = type; }
        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color accent = type.equals("brush") ? new Color(55, 120, 210) : type.equals("eraser") ? new Color(190, 85, 165) : type.equals("crop") ? new Color(215, 145, 30) : type.equals("text") ? new Color(70, 120, 190) : type.equals("shape") ? new Color(60, 155, 105) : type.equals("rotate") ? new Color(150, 95, 190) : type.equals("picker") ? new Color(35, 145, 135) : new Color(65, 70, 82);
            if ((type.equals("undo") || type.equals("redo")) && c instanceof AbstractButton button) accent = button.isEnabled() ? (type.equals("undo") ? new Color(45, 115, 210) : new Color(35, 155, 105)) : new Color(145, 150, 160);
            p.setColor(new Color(232, 235, 242)); p.fillRoundRect(x, y, 21, 21, 6, 6); p.setColor(new Color(195, 200, 210)); p.drawRoundRect(x, y, 21, 21, 6, 6); p.setColor(accent); p.setStroke(new BasicStroke(2));
            if (type.equals("undo")) { p.drawArc(x + 4, y + 5, 13, 12, 45, 250); p.fillPolygon(new int[]{x + 3, x + 9, x + 8}, new int[]{y + 10, y + 5, y + 13}, 3); }
            else if (type.equals("redo")) { p.drawArc(x + 4, y + 5, 13, 12, -135, 250); p.fillPolygon(new int[]{x + 18, x + 12, x + 13}, new int[]{y + 10, y + 5, y + 13}, 3); }
            else if (type.equals("move")) { p.drawLine(x + 5, y + 11, x + 17, y + 11); p.drawLine(x + 11, y + 5, x + 11, y + 17); p.fillPolygon(new int[]{x + 17, x + 13, x + 13}, new int[]{y + 11, y + 8, y + 14}, 3); p.fillPolygon(new int[]{x + 5, x + 9, x + 9}, new int[]{y + 11, y + 8, y + 14}, 3); p.fillPolygon(new int[]{x + 11, x + 8, x + 14}, new int[]{y + 5, y + 9, y + 9}, 3); p.fillPolygon(new int[]{x + 11, x + 8, x + 14}, new int[]{y + 17, y + 13, y + 13}, 3); }
            else if (type.equals("text")) { p.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16)); p.drawString("T", x + 6, y + 17); p.drawLine(x + 4, y + 19, x + 18, y + 19); }
            else if (type.equals("shape")) { p.drawRect(x + 4, y + 5, 9, 9); p.drawOval(x + 11, y + 11, 7, 7); }
            else if (type.equals("rotate")) { p.drawArc(x + 4, y + 4, 14, 14, 35, 285); p.fillPolygon(new int[]{x + 17, x + 12, x + 16}, new int[]{y + 4, y + 5, y + 9}, 3); }
            else if (type.equals("crop")) { p.drawLine(x + 4, y + 8, x + 4, y + 4); p.drawLine(x + 4, y + 4, x + 8, y + 4); p.drawLine(x + 14, y + 4, x + 18, y + 4); p.drawLine(x + 18, y + 4, x + 18, y + 8); p.drawLine(x + 4, y + 14, x + 4, y + 18); p.drawLine(x + 4, y + 18, x + 8, y + 18); p.drawLine(x + 14, y + 18, x + 18, y + 18); p.drawLine(x + 18, y + 18, x + 18, y + 14); }
            else if (type.equals("shadow")) { p.setColor(new Color(150, 155, 165)); p.fillRoundRect(x + 8, y + 8, 10, 10, 3, 3); p.setColor(accent); p.fillRoundRect(x + 4, y + 4, 10, 10, 3, 3); }
            else if (type.equals("select")) { p.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{3, 2}, 0)); p.drawRect(x + 4, y + 4, 14, 14); p.setStroke(new BasicStroke(2)); p.drawLine(x + 7, y + 2, x + 7, y + 6); }
            else if (type.equals("picker")) { p.drawLine(x + 6, y + 16, x + 16, y + 6); p.drawOval(x + 12, y + 3, 5, 5); p.drawLine(x + 5, y + 17, x + 9, y + 17); p.drawLine(x + 6, y + 14, x + 9, y + 17); }
            else if (type.equals("fill")) { p.drawLine(x + 6, y + 5, x + 15, y + 14); p.drawLine(x + 6, y + 5, x + 4, y + 11); p.drawLine(x + 15, y + 14, x + 12, y + 17); p.setColor(new Color(70, 140, 220)); p.fillArc(x + 10, y + 13, 8, 6, 180, 180); }
            else if (type.equals("gradient")) { p.setPaint(new GradientPaint(x + 4, y + 4, new Color(35, 75, 150), x + 18, y + 18, Color.WHITE)); p.fillRoundRect(x + 4, y + 4, 14, 14, 3, 3); }
            else if (type.equals("brush")) { p.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); p.drawLine(x + 6, y + 16, x + 16, y + 6); p.setStroke(new BasicStroke(1.5f)); p.drawLine(x + 5, y + 17, x + 9, y + 13); p.drawLine(x + 7, y + 19, x + 11, y + 15); }
            else if (type.equals("eraser")) { p.rotate(-Math.PI / 4, x + 11, y + 11); p.fillRoundRect(x + 5, y + 7, 13, 8, 3, 3); p.setColor(Color.WHITE); p.drawLine(x + 11, y + 7, x + 11, y + 15); p.rotate(Math.PI / 4, x + 11, y + 11); }
            else if (type.equals("color")) { p.setColor(new Color(220, 70, 70)); p.fillOval(x + 3, y + 3, 9, 9); p.setColor(new Color(65, 110, 220)); p.fillOval(x + 10, y + 8, 9, 9); p.setColor(new Color(70, 175, 100)); p.fillOval(x + 3, y + 11, 9, 9); }
            else if (type.equals("image")) { p.drawRoundRect(x + 4, y + 5, 14, 13, 2, 2); p.fillOval(x + 7, y + 7, 3, 3); p.drawLine(x + 5, y + 16, x + 10, y + 11); p.drawLine(x + 10, y + 11, x + 13, y + 14); p.drawLine(x + 13, y + 14, x + 16, y + 10); }
            else { p.drawRect(x + 4, y + 4, 12, 12); p.drawLine(x + 8, y + 4, x + 8, y + 8); p.drawLine(x + 12, y + 12, x + 16, y + 12); }
            p.dispose();
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new PhotoshopStudio().setVisible(true));
    }
}
