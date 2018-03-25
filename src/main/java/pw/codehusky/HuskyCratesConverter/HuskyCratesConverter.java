package pw.codehusky.HuskyCratesConverter;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

/*
 * FileChooserDemo.java uses these files:
 *   images/Open16.gif
 *   images/Save16.gif
 */
public class HuskyCratesConverter extends JPanel
        implements ActionListener {
    static private final String newline = "\n";
    JButton openButton, convertButton;
    JTextArea log;
    JFileChooser fc;

    public HuskyCratesConverter() {
        super(new BorderLayout());


        //Create a file chooser
        fc = new JFileChooser();

        //Uncomment one of the following lines to try a different
        //file selection mode.  The first allows just directories
        //to be selected (and, at least in the Java look and feel,
        //shown).  The second allows both files and directories
        //to be selected.  If you leave these lines commented out,
        //then the default mode (FILES_ONLY) will be used.
        //
        //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        openButton = new JButton("Open config file...");
        openButton.addActionListener(this);

        //Create the save button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        convertButton = new JButton("Convert!");
        convertButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);
        buttonPanel.add(convertButton);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }
    public ConfigurationLoader<CommentedConfigurationNode> crateConfigLoader;
    public ConfigurationLoader<CommentedConfigurationNode> crateConvertedLoader;
    public CommentedConfigurationNode crateConfig;
    public CommentedConfigurationNode convertedCrateConfig;
    public void actionPerformed(ActionEvent e) {

        //Handle open button action.
        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(HuskyCratesConverter.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                crateConfigLoader = HoconConfigurationLoader.builder().setFile(file).build();
                crateConvertedLoader = HoconConfigurationLoader.builder().setFile(new File(file.getPath().replace(file.getName(),"") + "converted.conf")).build();
                try {
                    crateConfig = crateConfigLoader.load();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null,e1.getMessage(),"Failure!",JOptionPane.PLAIN_MESSAGE);
                }
            }

            //Handle save button action.
        } else if (e.getSource() == convertButton) {
            convertedCrateConfig = crateConfigLoader.createEmptyNode();
            log.setText("-- Conversion starting --\n\n");

            if(crateConfig.getNode("crates").isVirtual()){
                log.append("-- Failed! --\nNo crates{} object found in this config. Aborting.\n");
                return;
            }

            for(ConfigurationNode oldCrateNode : crateConfig.getNode("crates").getChildrenMap().values()){
                String name = oldCrateNode.getNode("name").getString();
                String id = oldCrateNode.getKey().toString();
                String viewType = oldCrateNode.getNode("type").getString("spinner").toLowerCase();
                Boolean isFree = oldCrateNode.getNode("options","freeCrate").getBoolean(false);
                Long useCooldown = oldCrateNode.getNode("options","freeCrateDelay").getLong();


                log.append("-- Crate Information Begin --\nName: " + ((name != null)?name:"NONE") + "\nID: " + id + "\nItems: " + oldCrateNode.getNode("items").getChildrenList().size() + "\nFree: " + isFree.toString() + "\nCooldown Time: " + ((useCooldown > 0)?useCooldown:"None") + "\n-- Crate Information End --\n");

                ConfigurationNode crateNode = convertedCrateConfig.getNode(id);

                crateNode.getNode("name").setValue(name);
                crateNode.getNode("hologram","lines").getAppendedNode().setValue(name);

                crateNode.getNode("free").setValue(isFree);
                crateNode.getNode("cooldownSeconds").setValue(useCooldown);

                crateNode.getNode("viewType").setValue(viewType);
                if(!oldCrateNode.getNode("spinnerOptions").isVirtual()){
                    crateNode.getNode("viewOptions","tickDelayMultiplier").setValue(oldCrateNode.getNode("spinnerOptions","dampening").getValue());
                    crateNode.getNode("viewOptions","ticksToSelection").setValue(oldCrateNode.getNode("spinnerOptions","maxClicks").getValue());
                    Integer maxMod = null;
                    Integer minMod = null;
                    if(!oldCrateNode.getNode("spinnerOptions","maxClickModifier").isVirtual()){
                        maxMod = oldCrateNode.getNode("spinnerOptions","maxClickModifier").getInt();
                    }
                    if(!oldCrateNode.getNode("spinnerOptions","minClickModifier").isVirtual()){
                        minMod = oldCrateNode.getNode("spinnerOptions","minClickModifier").getInt();
                    }
                    if(maxMod != null && minMod != null){
                        crateNode.getNode("viewOptions","ticksToSelectionVariance").setValue(Math.max(Math.abs(maxMod),Math.abs(minMod)));
                    }else if(maxMod != null){
                        crateNode.getNode("viewOptions","ticksToSelectionVariance").setValue(Math.abs(maxMod));
                    }else if(minMod != null){
                        crateNode.getNode("viewOptions","ticksToSelectionVariance").setValue(Math.abs(minMod));
                    }
                }

                crateNode.getNode("useLocalKey").setValue(true);

                if(!oldCrateNode.getNode("options","keyID").isVirtual()){
                    crateNode.getNode("localKey","id").setValue(oldCrateNode.getNode("options","keyID").getValue());
                }

                log.append("\n");
            }

            log.append("-- Saving new config to converted.conf --\n");
            try {
                crateConvertedLoader.save(convertedCrateConfig);
            } catch (IOException e1) {
                log.append("-- Failed! --\n");
                log.append(e1.toString() + "\n");
                return;
            }
            log.append("-- Saved successfully. --\n");
        }
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = HuskyCratesConverter.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("HuskyCrates Config Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        frame.add(new HuskyCratesConverter());

        //Display the window.
        frame.pack();
        frame.setSize(400,600);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }
}