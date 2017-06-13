package pw.codehusky.HuskyCratesConverter;

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
            log.setText("");
            //convert!
            for(Object key : crateConfig.getNode("crates").getChildrenMap().keySet()){
                CommentedConfigurationNode oldCrate = crateConfig.getNode("crates",key);
                CommentedConfigurationNode crate = convertedCrateConfig.getNode("crates",key);
                log.append("--- Crate Info ---\n");
                if(oldCrate.getNode("items").getChildrenList().get(0).getNode("formatversion").isVirtual()){
                    log.append("config version: v0\n");
                }else{
                    log.append("config version: v" + oldCrate.getNode("items").getChildrenList().get(0).getNode("formatversion").getString("ERR") + "\n");
                }
                log.append("name: " + oldCrate.getNode("name").getString() + "\n");
                log.append("itemCount: " + oldCrate.getNode("items").getChildrenList().size() + "\n");
                log.append("--- Starting Conversion ---\n");
                convertedCrateConfig.getNode("crates",key,"name").setValue(oldCrate.getNode("name").getString("Unnamed Crate").replaceAll("§","&"));
                convertedCrateConfig.getNode("crates",key,"type").setValue(oldCrate.getNode("type").getString("Spinner"));
                java.util.List<? extends CommentedConfigurationNode> items = oldCrate.getNode("items").getChildrenList();
                double count = 0;
                DecimalFormat df = new DecimalFormat("#.00");
                for(CommentedConfigurationNode oldItem : items){
                    CommentedConfigurationNode item = crate.getNode("items").getAppendedNode();
                    item.getNode("name").setValue(oldItem.getNode("name").getString("Unnamed Crate").replaceAll("§","&"));
                    item.getNode("count").setValue(oldItem.getNode("amount").getValue());
                    item.getNode("huskydata","weight").setValue(oldItem.getNode("chance").getValue());
                    item.getNode("id").setValue(oldItem.getNode("id").getValue());
                    if(!oldItem.getNode("lore").isVirtual()) {
                        item.getNode("lore").getAppendedNode().setValue(oldItem.getNode("lore").getValue().toString().replaceAll("§", "&"));
                    }
                    item.getNode("formatversion").setValue(1);
                    CommentedConfigurationNode reward = item.getNode("huskydata","rewards").getAppendedNode();
                    if(!oldItem.getNode("command").isVirtual()){
                        reward.getNode("type").setValue("command");
                        reward.getNode("command").setValue(oldItem.getNode("command").getValue());
                    }else{
                        reward.getNode("type").setValue("item");
                    }
                    count++;
                    log.append(df.format((count / items.size())*100) + "%" + " -- " + oldItem.getNode("name").getValue()  + "\n");
                }
                log.append("--- Conversion Completed ---\n");
                //JOptionPane.showMessageDialog(null,"itemCount: " + crate.getNode("items").getChildrenList().size(),crate.getNode("name").getString(),JOptionPane.PLAIN_MESSAGE);
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