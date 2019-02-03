package pw.codehusky.HuskyCratesConverter;

import com.sun.javafx.image.IntPixelGetter;
import javafx.scene.control.Alert;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        fc.setFileFilter(new FileNameExtensionFilter("HuskyCrates v1.x Configurations (.conf)","conf"));

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
                crateConvertedLoader = HoconConfigurationLoader.builder().setFile(new File(file.getPath().replace(file.getName(),"") + "crates.converted.conf")).build();
                try {
                    crateConfig = crateConfigLoader.load();
                    log.setText("-- File ready --\n\n");
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null,e1.getMessage(),"Failure!",JOptionPane.PLAIN_MESSAGE);
                }
            }

            //Handle save button action.
        } else if (e.getSource() == convertButton) {
            new Thread(() -> {
                if(crateConfigLoader == null){
                    log.setText("!!! Please load a configuration before trying to convert !!!");
                    return;
                }
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
                    if(!oldCrateNode.getNode("options","showRewardsOnLeft").isVirtual()){
                        crateNode.getNode("previewable").setValue(oldCrateNode.getNode("options","showRewardsOnLeft").getValue());
                    }
                    if(!oldCrateNode.getNode("options","scrambleRewards").isVirtual()){
                        crateNode.getNode("viewConfig","scrambleSlots").setValue(oldCrateNode.getNode("options","scrambleRewards").getValue());
                    }
                    if(!oldCrateNode.getNode("options","keyID").isVirtual()){
                        crateNode.getNode("localKey","id").setValue(oldCrateNode.getNode("options","keyID").getValue());
                        crateNode.getNode("localKey","name").setValue(name + "&r Key");
                    }

                    List<Integer> color1 = Arrays.asList(255,0,0);
                    List<Integer> color2 = Arrays.asList(100,100,100);
                    if(!oldCrateNode.getNode("options","particle1").isVirtual()){
                        color1 = Arrays.asList(oldCrateNode.getNode("options","particle1","color",0).getInt(255),
                                oldCrateNode.getNode("options","particle1","color",1).getInt(0),
                                oldCrateNode.getNode("options","particle1","color",2).getInt(0));
                    }
                    if(!oldCrateNode.getNode("options","particle2").isVirtual()){
                        color2 = Arrays.asList(oldCrateNode.getNode("options","particle2","color",0).getInt(100),
                                oldCrateNode.getNode("options","particle2","color",1).getInt(100),
                                oldCrateNode.getNode("options","particle2","color",2).getInt(100));
                    }
                    ConfigurationNode effectNode = crateNode.getNode("effects","idle");

                    ConfigurationNode particle1 = effectNode.getNode("particles").getAppendedNode();
                    particle1.getNode("type").setValue("minecraft:redstone_dust");
                    particle1.getNode("animationPreset").setValue("orbit");
                    particle1.getNode("color").getAppendedNode().setValue(color1.get(0));
                    particle1.getNode("color").getAppendedNode().setValue(color1.get(1));
                    particle1.getNode("color").getAppendedNode().setValue(color1.get(2));

                    ConfigurationNode particle2 = effectNode.getNode("particles").getAppendedNode();
                    particle2.getNode("type").setValue("minecraft:redstone_dust");
                    particle2.getNode("animationPreset").setValue("orbit");
                    particle2.getNode("color").getAppendedNode().setValue(color2.get(0));
                    particle2.getNode("color").getAppendedNode().setValue(color2.get(1));
                    particle2.getNode("color").getAppendedNode().setValue(color2.get(2));
                    log.append("\n-- Crate info Converted. Converting items now. --\n");
                    for(ConfigurationNode item : oldCrateNode.getNode("items").getChildrenList()){
                        ConfigurationNode slot = crateNode.getNode("slots").getAppendedNode();

                        slot.getNode("chance").setValue(item.getNode("huskydata","weight").getDouble(1));

                        ConfigurationNode displayItem = slot.getNode("displayItem");
                        displayItem.getNode("count").setValue(item.getNode("count").getValue());
                        String oldid = item.getNode("id").getString("");
                        String[] chunks = oldid.split(":");
                        boolean damageInferred=false;
                        try {
                            displayItem.getNode("damage").setValue(Integer.parseInt(chunks[chunks.length - 1]));
                            damageInferred=true;
                        }catch(NumberFormatException bepoe){
                            displayItem.getNode("damage").setValue(item.getNode("damage").getValue());
                        }
                        if(damageInferred){
                            displayItem.getNode("id").setValue((chunks.length > 1)?(chunks[0] + ":" + chunks[1]):chunks[0]);
                        }else {
                            displayItem.getNode("id").setValue(item.getNode("id").getValue());
                        }
                        displayItem.getNode("lore").setValue(item.getNode("lore").getValue());
                        displayItem.getNode("name").setValue(item.getNode("name").getValue());
                        displayItem.getNode("enchantments").setValue(item.getNode("enchants").getValue());
                        displayItem.getNode("nbt").setValue(item.getNode("nbt").getValue());
                        boolean announce = item.getNode("huskydata","announce").getBoolean(false);
                        for(ConfigurationNode oldReward : item.getNode("huskydata","rewards").getChildrenList()){
                            ConfigurationNode reward = slot.getNode("rewards").getAppendedNode();
                            switch(oldReward.getNode("type").getString()){
                                case "command":
                                    reward.getNode("type").setValue("servercommand");
                                    if(oldReward.getNode("command").isVirtual()){
                                        log.append("!!WARNING!! A command was set to \"INVALID COMMAND FROM HUSKYCONVERTER\". Please fix this manually.\n");
                                    }
                                    reward.getNode("data").setValue(oldReward.getNode("command").getString("say INVALID COMMAND FROM HUSKYCONVERTER"));
                                    break;
                                case "item":
                                    reward.getNode("type").setValue("item");
                                    if(!oldReward.getNode("overrideItem").isVirtual()){
                                        ConfigurationNode oldRewardItem = oldReward.getNode("overrideItem");
                                        ConfigurationNode rewardItem = reward.getNode("item");
                                        rewardItem.getNode("count").setValue(oldRewardItem.getNode("count").getValue());
                                        rewardItem.getNode("id").setValue(oldRewardItem.getNode("id").getValue()); //TODO: CHECK IF WE NEED TO STRIP DAMAGE VALUE
                                        rewardItem.getNode("lore").setValue(oldRewardItem.getNode("lore").getValue());
                                        rewardItem.getNode("name").setValue(oldRewardItem.getNode("name").getValue());
                                        rewardItem.getNode("enchantments").setValue(oldRewardItem.getNode("enchants").getValue());
                                        rewardItem.getNode("nbt").setValue(oldRewardItem.getNode("nbt").getValue());
                                    }else if(!oldReward.getNode("overrideCount").isVirtual()){
                                        reward.getNode("item").setValue(displayItem);
                                        reward.getNode("item","count").setValue(oldReward.getNode("overrideCount").getInt(1));
                                    }
                                    break;
                            }
                        }
                        if(announce){
                            ConfigurationNode announceReward = slot.getNode("rewards").getAppendedNode();
                            announceReward.getNode("type").setValue("servermessage");
                            announceReward.getNode("data").setValue(
                                    (
                                        (!oldCrateNode.getNode("lang","rewardAnnounceMessage").isVirtual())?
                                                oldCrateNode.getNode("lang","rewardAnnounceMessage").getString("%p just won %R&r from a %C&r!"):
                                                crateConfig.getNode("lang","rewardAnnounceMessage").getString("%p just won %R&r from a %C&r!")
                                    ).replace("%c",name).replace("%prefix%", (
                                            (!oldCrateNode.getNode("lang","prefix").isVirtual())?
                                                    oldCrateNode.getNode("lang","prefix").getString(""):
                                                    crateConfig.getNode("lang","prefix").getString("")
                                    )).replace("%r",item.getNode("name").getString()));
                        }

                        ConfigurationNode winNotice = slot.getNode("rewards").getAppendedNode();
                        winNotice.getNode("type").setValue("usermessage");
                        winNotice.getNode("data").setValue(
                                (
                                        (!oldCrateNode.getNode("lang","rewardMessage").isVirtual())?
                                                oldCrateNode.getNode("lang","rewardMessage").getString("%prefix%You won %a %R&r from a %C&r!"):
                                                crateConfig.getNode("lang","rewardMessage").getString("%prefix%You won %a %R&r from a %C&r!")
                                ).replace("%c",name).replace("%prefix%", ((!oldCrateNode.getNode("lang","prefix").isVirtual())?oldCrateNode.getNode("lang","prefix").getString(""):crateConfig.getNode("lang","prefix").getString("") )).replace("%r",item.getNode("name").getString()));


                    }
                    log.append("-- Converted " + oldCrateNode.getNode("items").getChildrenList().size() + " items --\n");
                    log.append("\n-- Converting lang --\n");
                    log.append("\n-- Finished converting lang --\n");
                    log.append("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
                }

                log.append("-- Saving new config --\n");
                try {
                    crateConvertedLoader.save(convertedCrateConfig);
                    JOptionPane.showMessageDialog(null, "crates.converted.conf has been saved successfully.","Conversion Success",JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e1) {
                    log.append("-- Failed! --\n");
                    log.append(e1.toString() + "\n");
                    return;
                }
                log.append("Converted output saved to crates.converted.conf\n");
            }).run();
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
        JFrame frame = new JFrame("HuskyCrates Config Converter v0.4.1");
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