package ro.thehunters.digi.recipeManager;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import ro.thehunters.digi.recipeManager.recipes.BaseRecipe;
import ro.thehunters.digi.recipeManager.recipes.BaseRecipe.RecipeType;
import ro.thehunters.digi.recipeManager.recipes.CombineRecipe;
import ro.thehunters.digi.recipeManager.recipes.CraftRecipe;
import ro.thehunters.digi.recipeManager.recipes.FuelRecipe;
import ro.thehunters.digi.recipeManager.recipes.ItemResult;
import ro.thehunters.digi.recipeManager.recipes.RecipeInfo;
import ro.thehunters.digi.recipeManager.recipes.RecipeInfo.RecipeOwner;
import ro.thehunters.digi.recipeManager.recipes.SmeltRecipe;
import ro.thehunters.digi.recipeManager.recipes.flags.Flag;
import ro.thehunters.digi.recipeManager.recipes.flags.Flags;
import ro.thehunters.digi.recipeManager.recipes.flags.ItemFlags;
import ro.thehunters.digi.recipeManager.recipes.flags.RecipeFlags;

/**
 * Processes all recipe files and updates main Recipes class once done.
 */
public class RecipeProcessor implements Runnable
{
    @Override
    protected void finalize() throws Throwable // TODO REMOVE
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + getClass().getName() + " :: finalize()");
        
        super.finalize();
    }
    
    private final CommandSender                 sender;
    private final boolean                       check;
    private final boolean                       force;
    
    private String                              currentFile;
    private BufferedReader                      reader;
    private boolean                             commentBlock;
    private RecipeFlags                         fileFlags;
    private String                              line;
    private int                                 lineNum;
    private int                                 loaded;
    
    // Storage
    private volatile RecipeRegistrator          registrator   = null;
    private final Set<String>                   foundFiles    = new HashSet<String>();
    private final List<String>                  fileList      = new ArrayList<String>();
    private final HashMap<String, List<String>> recipeErrors  = new HashMap<String, List<String>>();
    
    // Static storage
    private final static Map<String, Integer>   lastModified  = new HashMap<String, Integer>();
    
    // Constants
    private final String                        DIR_PLUGIN    = RecipeManager.getPlugin().getDataFolder() + File.separator;
    private final String                        DIR_RECIPES   = DIR_PLUGIN + "recipes" + File.separator;
//    private final String                      FILE_LASTREAD = DIR_RECIPES + "lastread.dat";
    private final String                        FILE_ERRORLOG = DIR_PLUGIN + "last recipe errors.log";
    private final String                        NL            = System.getProperty("line.separator");
    private final String[]                      COMMENTS      = { "//", "#" };
    
    private static BukkitTask                   task;
    
    protected static void reload(CommandSender sender, boolean check, boolean force)
    {
        new RecipeProcessor(sender, check, force);
    }
    
    private RecipeProcessor(CommandSender sender, boolean check, boolean force)
    {
        this.sender = sender;
        this.check = check;
        this.force = force;
        
        if(task != null)
            task.cancel();
        
        if(RecipeManager.getSettings().MULTITHREADING)
            task = Bukkit.getScheduler().runTaskAsynchronously(RecipeManager.getPlugin(), this);
        else
            run();
    }
    
    @Override
    public void run()
    {
        final long start = System.currentTimeMillis();
        
        try
        {
            Messages.send(sender, (check ? "Checking" : "Loading") + " " + (force ? "all" : "changed") + " recipes...");
            
            File dir = new File(DIR_RECIPES);
            
            if(!dir.exists() && !dir.mkdirs())
                Messages.send(sender, ChatColor.RED + "Error: couldn't create directories: " + dir.getPath());
            
            if(force)
                lastModified.clear();
            
            // Scan for files
            analyzeDirectory(dir);
            
            boolean noRecipeFiles = foundFiles.isEmpty();
            
            if(fileList.isEmpty())
            {
                if(noRecipeFiles)
                {
                    Messages.send(sender, "Done (" + (System.currentTimeMillis() - start) / 1000.0 + "s), no recipe files exist in the recipes folder!");
                }
                else
                {
                    Messages.send(sender, "Done (" + (System.currentTimeMillis() - start) / 1000.0 + "s), no modified recipe files to " + (check ? "check" : "load") + ".");
                    
                    if(!force)
                        Messages.send(sender, "You can use 'force' argument for 'rmreload' command to re-check all files regardless of modified state.");
                }
            }
            else
            {
                registrator = new RecipeRegistrator(sender);
                
                long lastDisplay = System.currentTimeMillis();
                long time;
                int numFiles = fileList.size();
                int parsedFiles = 0;
                loaded = 0;
                
                // Start reading files...
                for(String name : fileList)
                {
                    try
                    {
                        parseFile(DIR_RECIPES, name);
                        parsedFiles++;
                        time = System.currentTimeMillis();
                        
                        // display progress each second
                        if(time > lastDisplay + 500)
                        {
                            Messages.send(sender, "Recipes processed " + ((parsedFiles * 100) / numFiles) + "%...");
                            lastDisplay = time;
                        }
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                
                int errors = recipeErrors.size();
                
                if(errors > 0)
                {
                    Messages.send(sender, ChatColor.YELLOW + "Done " + (check ? "checking" : "loading") + " " + loaded + " recipes in " + (System.currentTimeMillis() - start) / 1000.0 + " seconds, " + errors + " errors were found" + (sender == null ? ", see below:" : ", see console."));
                    
                    StringBuilder buffer = new StringBuilder(errors * 128).append(ChatColor.RED).append("There were ").append(errors).append(" errors while processing the files: ").append(NL).append(NL);
                    String lastError;
                    int lastErrorNum;
                    int similarErrors;
                    
                    for(Entry<String, List<String>> entry : recipeErrors.entrySet())
                    {
                        buffer.append(ChatColor.BOLD).append(ChatColor.BLUE).append("File: ").append(entry.getKey()).append(NL);
                        
                        lastError = "";
                        lastErrorNum = 0;
                        similarErrors = 0;
                        
                        for(String error : entry.getValue())
                        {
                            if(error.startsWith(lastError, 10))
                            {
                                if(++lastErrorNum > 10)
                                {
                                    similarErrors++;
                                    continue;
                                }
                            }
                            else
                            {
                                if(similarErrors > 0)
                                    buffer.append(ChatColor.RED).append("... and ").append(similarErrors).append(" more similar errors.").append(NL);
                                
                                lastErrorNum = 0;
                                similarErrors = 0;
                            }
                            
                            buffer.append(ChatColor.WHITE).append(error).append(NL);
                            lastError = error.substring(10);
                        }
                        
                        if(similarErrors > 0)
                            buffer.append(ChatColor.RED).append("... and ").append(similarErrors).append(" more similar errors.").append(NL);
                        
                        buffer.append(NL);
                    }
                    
                    Messages.info(buffer.append(NL).append(NL).toString());
                    
                    if(Tools.saveTextToFile(ChatColor.stripColor(buffer.toString()), FILE_ERRORLOG))
                        Messages.info(ChatColor.YELLOW + "Apart from server.log, these errors have been saved in '" + FILE_ERRORLOG + "' as well.");
                }
                else
                {
                    Messages.send(sender, "Done " + (check ? "checking" : "loading") + " " + loaded + " recipes without errors, elapsed time " + (System.currentTimeMillis() - start) / 1000.0 + " seconds.");
                }
                
                if(!lastModified.isEmpty())
                {
                    // Clean up last modified list of inexistent files
                    Iterator<Entry<String, Integer>> iterator = lastModified.entrySet().iterator();
                    String fileName;
                    
                    while(iterator.hasNext())
                    {
                        fileName = iterator.next().getKey();
                        
                        if(!foundFiles.contains(fileName))
                        {
                            foundFiles.remove(fileName);
                            iterator.remove();
                        }
                    }
                }
            }
        }
        finally
        {
            task = null;
            
            if(check || registrator == null)
                return;
            
            // Calling registerRecipesToServer() in main thread...
            if(RecipeManager.getSettings().MULTITHREADING)
            {
                Bukkit.getScheduler().runTask(RecipeManager.getPlugin(), new Runnable()
                {
                    public void run()
                    {
                        registrator.registerRecipesToServer(sender, start, (fileList.size() == foundFiles.size() ? null : new HashSet<String>(fileList)));
                    }
                });
            }
            else
            {
                registrator.registerRecipesToServer(sender, start, (fileList.size() == foundFiles.size() ? null : new HashSet<String>(fileList)));
            }
        }
    }
    
    private void analyzeDirectory(File dir)
    {
        String fileName;
        Integer lastMod;
        int fileMod;
        
        for(File file : dir.listFiles())
        {
            if(file.isDirectory())
            {
                if(!file.getName().equalsIgnoreCase("disabled"))
                    analyzeDirectory(file);
            }
            else if(file.getName().endsWith(".txt"))
            {
                fileName = file.getPath().replace(DIR_RECIPES, ""); // get the relative path+filename
                foundFiles.add(fileName); // add to found files list to clean the lastmodified file later
                
                fileMod = Math.round(file.lastModified() / 1000);
                lastMod = lastModified.get(fileName);
                
                if(lastMod == null)
                    lastModified.put(fileName, fileMod);
                else if(lastMod == fileMod)
                    continue;
                
                fileList.add(fileName); // add to the processing file list
            }
        }
    }
    
    // TODO remove this
    private void debug(String message)
    {
//        StringBuilder msg = new StringBuilder().append(ChatColor.RED).append("[debug] ").append(ChatColor.LIGHT_PURPLE).append(message).append(" | ").append(ChatColor.GOLD).append(lineNum).append(" | ").append(line);
        
//        Bukkit.getConsoleSender().sendMessage(msg.toString());
    }
    
    private void parseFile(String root, String fileName) throws Exception
    {
        reader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(root + fileName))));
        currentFile = fileName;
        lineNum = 0;
        fileFlags = new RecipeFlags();
        commentBlock = false;
        String[] error = null;
        
        parseFlags(fileFlags, null, null); // parse file header flags that applies to all recipes
        
        while(searchRecipes()) // search for recipes...
        {
            debug("checking recipe type...");
            
            if(line.equalsIgnoreCase(RecipeType.CRAFT.getDirective()))
                error = parseCraftRecipe();
            else if(line.equalsIgnoreCase(RecipeType.COMBINE.getDirective()))
                error = parseCombineRecipe();
            else if(line.equalsIgnoreCase(RecipeType.SMELT.getDirective()))
                error = parseSmeltRecipe();
            else if(line.equalsIgnoreCase(RecipeType.FUEL.getDirective()))
                error = parseFuelRecipe();
            else if(line.equalsIgnoreCase("removeresult"))
                error = parseRemoveResult();
            else
                error = new String[] { ChatColor.YELLOW + "Unexpected directive: '" + line + "'", "This might be caused by previous errors. For more info read '" + Files.FILE_INFO_ERRORS + "'." };
            
            if(error != null)
                recipeError(error[0], (error.length > 1 ? error[1] : null));
        }
        
        if(lineNum == 0)
            recipeError(ChatColor.YELLOW + "Recipe file '" + fileName + "' is empty.", null);
        
        reader.close();
    }
    
    private boolean searchRecipes()
    {
        if(line != null)
        {
            for(RecipeType type : RecipeType.values())
            {
                if(line.equalsIgnoreCase(type.getDirective()))
                    return true;
            }
        }
        
        return nextLine();
    }
    
    private boolean readNextLine()
    {
        lineNum++;
        
        try
        {
            line = reader.readLine();
            return line != null;
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean nextLine()
    {
        do
        {
            if(!readNextLine())
                return false;
            
            line = parseComments();
        }
        while(line == null);
        
        return true;
    }
    
    private String parseComments()
    {
//        debug("parsing comments...");
        
        line = (line == null ? null : line.trim());
        
        if(line == null || line.isEmpty())
            return null;
        
        int index;
        
        // if we are in a comment block check for exit character
        if(commentBlock)
        {
            index = line.indexOf("*/");
            
            if(index >= 0)
            {
                commentBlock = false;
                return (index == 0 ? null : line.substring(0, index).trim());
            }
            
            return null;
        }
        
        index = line.indexOf("/*"); // check for comment block start
        
        if(index >= 0)
        {
            int end = line.indexOf("*/"); // check for comment block end chars on the same line
            
            if(end > 0)
            {
                return line.substring(0, index) + line.substring(end + 2);
            }
            else
            {
                commentBlock = true;
                return (index == 0 ? null : line.substring(0, index).trim());
            }
        }
        
        // now check for line comments
        for(String comment : COMMENTS)
        {
            index = line.indexOf(comment);
            
            if(index == 0)
                return null;
            
            if(index > -1)
                return line.substring(0, index).trim(); // partial comment, return filtered data
        }
        
        return line;
    }
    
    private void parseFlags(Flags flags, RecipeType recipeType, ItemStack item) throws Exception
    {
        debug("parsing flags...");
        
        nextLine();
        
        while(line != null && line.charAt(0) == '@')
        {
            parseFlag(flags, recipeType, item);
            nextLine();
        }
    }
    
    private void parseFlag(Flags flags, RecipeType recipeType, ItemStack item)
    {
        debug("parsing found flag...");
        
        String[] split = line.split(" ", 2);
        String flag = split[0].substring(1).trim().toLowerCase();
        
        int index = flag.indexOf(':');
        
        if(index >= 0)
        {
            flag = flag.substring(0, index);
//            recipeError(ChatColor.YELLOW + "Note: flag @" + flag + " has : character after it, you can now use flags without it !", null);
        }
        
        String value = (split.length > 1 ? split[1].trim() : null);
        
        boolean isOverrideFlag = flag.equals("override") || flag.equals("overwrite") || flag.equals("supercede") || flag.equals("replace");
        boolean isLogFlag = flag.equals("log");
        boolean isRemoveFlag = flag.equals("remove") || flag.equals("delete");
        boolean isCloneFlag = flag.equals("clone");
        boolean isSecretFlag = flag.equals("secret");
        
        if(value == null && !(isOverrideFlag || isLogFlag || isRemoveFlag || isCloneFlag || isSecretFlag))
        {
            recipeError("The @" + flag + " doesn't have a value set!", null);
            return;
        }
        
        // ___________________________________________________________________________________
        // Shared flags...
        
        if(flag.equals("test")) // TODO remove <<<
        {
            flags.test = (value == null ? true : !value.equalsIgnoreCase("false"));
            return;
        }
        
        if(flag.equals("message") || flag.equals("craftmsg"))
        {
            flags.setCraftMessage(value == null ? null : Tools.parseColors(value.replaceAll("\\", "\n"), false));
            return;
        }
        
        if(isSecretFlag)
        {
            flags.setSecret(value == null ? true : !value.equalsIgnoreCase("false"));
            return;
        }
        
        if(isLogFlag)
        {
            flags.setLog(value == null ? true : !value.equalsIgnoreCase("false"));
            return;
        }
        
        if(flag.equals("command") || flag.equals("commands"))
        {
            if(value.equalsIgnoreCase("false"))
                flags.setCommands(null);
            else
                flags.addCommand(value);
            
            return;
        }
        
        if(flag.equals("exp") || flag.equals("xp"))
        {
            split = value.split("\\|");
            value = split[0].trim();
            String failMessage = null;
            String successMessage = null;
            Integer val = null;
            
            if(!value.equalsIgnoreCase("false"))
            {
                if(split.length > 1)
                {
                    failMessage = split[1].trim();
                    
                    if(split.length > 2)
                        successMessage = split[2].trim();
                }
                
                try
                {
                    val = Integer.valueOf(value);
                }
                catch(Exception e)
                {
                    recipeError("The @" + flag + " flag has invalid number value '" + value + "'", null);
                    return;
                }
                
                if(val == 0)
                    val = null;
            }
            
            flags.setExpAward(val == null ? null : new Flag<Integer>(val, failMessage, successMessage));
            
            return;
        }
        
        if(flag.equals("launchfirework"))
        {
            if(value.equalsIgnoreCase("false"))
            {
                flags.setLaunchFirework(null);
                return;
            }
            
            FireworkMeta firework = flags.getLaunchFirework();
            
            if(firework == null)
                firework = (FireworkMeta)Bukkit.getItemFactory().getItemMeta(Material.FIREWORK);
            
            if(value.startsWith("effect"))
            {
                split = value.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has no arguments for 'effect' !", null);
                    return;
                }
                
                FireworkEffect effect = parseFireworkEffect(split[1].trim(), flag);
                
                if(effect != null)
                {
                    firework.addEffect(effect);
                    flags.setLaunchFirework(firework);
                }
            }
            else if(value.startsWith("power"))
            {
                split = value.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has no arguments for 'power' !", null);
                    return;
                }
                
                int power = -1;
                
                try
                {
                    power = Integer.valueOf(split[1].trim());
                }
                catch(Exception e)
                {
                }
                
                if(power < 0 || power > 128)
                {
                    recipeError("Flag @" + flag + " invalid 'power' argument, it must be a number from 0 to 128", null);
                    return;
                }
                
                firework.setPower(power);
                flags.setLaunchFirework(firework);
            }
            
            return;
        }
        
        // ___________________________________________________________________________________
        // Recipe-only flags...
        
        RecipeFlags recipeFlags = (flags instanceof RecipeFlags ? (RecipeFlags)flags : null);
        
        if(flag.equals("info") || flag.equals("recipeinfo"))
        {
            if(item != null)
                recipeError("Flag @" + flag + " can only be used on recipes!", null);
            else
                recipeFlags.setInfo(value.equalsIgnoreCase("false") ? null : value);
            
            return;
        }
        
        if(flag.equals("failmessage") || flag.equals("failmsg"))
        {
            if(item != null)
                recipeError("Flag @" + flag + " can only be used on recipes!", null);
            else
                recipeFlags.setFailMessage(value == null ? null : Tools.parseColors(value.replaceAll("\\n", "\n"), false));
            
            return;
        }
        
        if(isOverrideFlag)
        {
            if(item != null)
                recipeError("Flag @" + flag + " can only be used on recipes!", null);
            else
                recipeFlags.setOverride(value == null ? true : !value.equalsIgnoreCase("false"));
            
            return;
        }
        
        if(isRemoveFlag)
        {
            if(item != null)
                recipeError("Flag @" + flag + " can only be used on recipes!", null);
            else
                recipeFlags.setRemove(value == null ? true : !value.equalsIgnoreCase("false"));
            
            return;
        }
        
        if(flag.equals("restrict") || flag.equals("denied"))
        {
            if(item != null)
                recipeError("Flag @" + flag + " can only be used on recipes!", null);
            else
                recipeFlags.setRestrict(value == null || value.equalsIgnoreCase("false") ? null : value);
            
            return;
        }
        
        // ___________________________________________________________________________________
        // Result items only flags...
        
        ItemFlags itemFlags = (flags instanceof ItemFlags ? (ItemFlags)flags : null);
        
        if(flag.equals("name") || flag.equals("itemname"))
        {
            if(item != null)
            {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(value.equalsIgnoreCase("false") ? null : Tools.parseColors(value, false));
                item.setItemMeta(meta);
            }
            else
                recipeError("Flag @" + flag + " only works on result items.", null);
            
            return;
        }
        
        if(flag.equals("lore") || flag.equals("itemlore"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(value.equalsIgnoreCase("false"))
            {
                meta.setLore(null);
                item.setItemMeta(meta);
                return;
            }
            
            List<String> lore = meta.getLore();
            
            if(lore == null || lore.isEmpty())
                lore = new ArrayList<String>();
            
            lore.add(Tools.parseColors(value, false));
            
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            
            return;
        }
        
        if(flag.equals("color") || flag.equals("colour") || flag.equals("itemcolor") || flag.equals("itemcolour"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(meta instanceof LeatherArmorMeta == false)
            {
                recipeError("Flag @" + flag + " needs a leather armor item to work!", null);
                return;
            }
            
            Color color = null;
            
            if(!value.equalsIgnoreCase("false"))
            {
                color = Tools.parseColor(value);
                
                if(color == null)
                {
                    recipeError("Flag @" + flag + " has invalid color numbers!", "Use 3 numbers ranging from 0 to 255, e.g. 255 128 0 for orange.");
                    return;
                }
            }
            
            LeatherArmorMeta leather = (LeatherArmorMeta)meta;
            
            leather.setColor(color);
            
            item.setItemMeta(meta);
            
            return;
        }
        
        if(flag.equals("book") || flag.equals("itembook"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(meta instanceof BookMeta == false)
            {
                recipeError("Flag @" + flag + " needs a WRITTEN_BOOK item to work!", null);
                return;
            }
            
            BookMeta book = (BookMeta)meta;
            
            if(!value.equalsIgnoreCase("false"))
            {
                split = value.split("\\|", 2);
                
                if(split.length == 2)
                {
                    String title = Tools.parseColors(split[0].trim(), false);
                    String author = Tools.parseColors(split[1].trim(), false);
                    
                    if(title.length() > 64 || author.length() > 64)
                    {
                        recipeError("Flag @" + flag + " has title or author larger than 64 characters, that's too long!", null);
                        return;
                    }
                    
                    book.setTitle(title);
                    book.setAuthor(author);
                }
                else
                {
                    recipeError("Flag @" + flag + " doesn't have title | author argument format!", null);
                    return;
                }
            }
            else
            {
                book.setTitle(null);
                book.setAuthor(null);
            }
            
            item.setItemMeta(book);
            
            return;
        }
        
        if(flag.equals("page") || flag.equals("addpage") || flag.equals("bookpage"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(meta instanceof BookMeta == false)
            {
                recipeError("Flag @" + flag + " needs a WRITTEN_BOOK item to work!", null);
                return;
            }
            
            BookMeta book = (BookMeta)meta;
            
            if(value.equalsIgnoreCase("false"))
            {
                String text = Tools.parseColors(value, false);
                
                if(text.length() > 256)
                {
                    recipeError("Flag @" + flag + " has page with text longer than 256 characters, that's too long!", "Color codes use up that limit too, 1 character per color.");
                    return;
                }
                
                book.addPage(text);
            }
            else
            {
                book.getPages().clear();
            }
            
            item.setItemMeta(book);
            
            return;
        }
        
        if(flag.equals("firework") || flag.equals("fireworkrocket"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(meta instanceof FireworkMeta == false)
            {
                recipeError("Flag @" + flag + " needs a FIREWORK item to work!", null);
                return;
            }
            
            FireworkMeta firework = (FireworkMeta)meta;
            
            if(value.equalsIgnoreCase("false"))
            {
                firework.clearEffects();
                firework.setPower(0);
                item.setItemMeta(firework);
                return;
            }
            
            if(value.startsWith("effect"))
            {
                split = value.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has no arguments for 'effect' !", null);
                    return;
                }
                
                FireworkEffect effect = parseFireworkEffect(split[1].trim(), flag);
                
                if(effect != null)
                {
                    firework.addEffect(effect);
                    item.setItemMeta(firework);
                }
            }
            else if(value.startsWith("power"))
            {
                split = value.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has no arguments for 'power' !", null);
                    return;
                }
                
                int power = -1;
                
                try
                {
                    power = Integer.valueOf(split[1].trim());
                }
                catch(Exception e)
                {
                }
                
                if(power < 0 || power > 128)
                {
                    recipeError("Flag @" + flag + " invalid 'power' argument, it must be a number from 0 to 128", null);
                    return;
                }
                
                firework.setPower(power);
                item.setItemMeta(firework);
            }
            else
                recipeError("Flag @" + flag + " has unknown argument: " + value, null);
            
            return;
        }
        
        if(flag.equals("fireworkcharge") || flag.equals("fireworkeffect"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(meta instanceof FireworkEffectMeta == false)
            {
                recipeError("Flag @" + flag + " needs a FIREWORK_CHARGE item to work!", null);
                return;
            }
            
            FireworkEffectMeta firework = (FireworkEffectMeta)meta;
            
            if(value.equalsIgnoreCase("false"))
            {
                firework.setEffect(null);
                item.setItemMeta(firework);
                return;
            }
            
            FireworkEffect effect = parseFireworkEffect(value, flag);
            
            if(effect != null)
            {
                firework.setEffect(effect);
                item.setItemMeta(firework);
            }
            
            return;
        }
        
        if(flag.equals("skullowner") || flag.equals("headowner"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(meta instanceof SkullMeta == false || item.getDurability() != 3)
            {
                recipeError("Flag @" + flag + " needs a SKULL_ITEM with data value 3 to work!", null);
                return;
            }
            
            SkullMeta skull = (SkullMeta)meta;
            
            skull.setOwner(value.equalsIgnoreCase("false") ? null : value);
            
            item.setItemMeta(skull);
            
            return;
        }
        
        if(flag.equals("potion"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(meta instanceof PotionMeta == false)
            {
                recipeError("Flag @" + flag + " needs a POTION item to work!", null);
                return;
            }
            
            PotionMeta potion = (PotionMeta)meta;
            
            if(value.equalsIgnoreCase("false"))
            {
                potion.setMainEffect(null);
                potion.clearCustomEffects();
                item.setItemMeta(potion);
                item.setDurability((short)0);
            }
            else if(value.startsWith("custom"))
            {
                split = value.split(" ", 2);
                
                if(split.length != 2)
                {
                    recipeError("Flag @" + flag + " has no custom with no value defined!", null);
                    return;
                }
                
                value = split[1].trim();
                PotionEffect effect = parsePotionEffect(value, flag);
                
                if(effect != null)
                {
                    potion.addCustomEffect(effect, true);
                    item.setItemMeta(potion);
                }
            }
            else
            {
                Potion p = parsePotion(value, flag);
                
                if(p != null)
                {
                    item.setDurability(p.toDamageValue());
                }
            }
            
            return;
        }
        
        boolean isEnchantBook = flag.equals("enchantbook") || flag.equals("enchantedbook");
        
        if(isEnchantBook || flag.equals("enchant") || flag.equals("enchantment"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            if(isEnchantBook && meta instanceof EnchantmentStorageMeta == false)
            {
                recipeError("Flag @" + flag + " needs an ENCHANTED_BOOK item to work!", null);
                return;
            }
            
            EnchantmentStorageMeta enchants = null;
            
            if(isEnchantBook)
                enchants = (EnchantmentStorageMeta)meta;
            
            if(value.equalsIgnoreCase("false"))
            {
                if(isEnchantBook)
                    enchants.getStoredEnchants().clear();
                else
                    item.getEnchantments().clear();
                
                return;
            }
            
            split = value.split(" ");
            value = split[0].trim();
            
            Enchantment ench = Enchantment.getByName(value);
            
            if(ench == null)
            {
                recipeError("Flag @" + flag + " has invalid enchantment: " + value, "Read '" + Files.FILE_INFO_NAMES + "' for enchantment names.");
                return;
            }
            
            int level = ench.getStartLevel();
            
            if(split.length > 1)
            {
                value = split[1].trim();
                
                if(!value.equalsIgnoreCase("max"))
                {
                    try
                    {
                        level = Integer.valueOf(value);
                    }
                    catch(Exception e)
                    {
                        recipeError("Flag @" + flag + " has invalid enchantment level number!", null);
                        return;
                    }
                }
                else
                    level = ench.getMaxLevel();
            }
            
            if(isEnchantBook)
            {
                enchants.addStoredEnchant(ench, level, true);
                item.setItemMeta(enchants);
            }
            else
                item.addUnsafeEnchantment(ench, level);
            
            return;
        }
        
        if(flag.equals("map"))
        {
            if(item == null)
            {
                recipeError("Flag @" + flag + " only works on result items.", null);
                return;
            }
            
            if(item.getType() != Material.MAP)
            {
                recipeError("Flag @" + flag + " needs a MAP item to work!", null);
                return;
            }
            
            if(value.equalsIgnoreCase("false"))
            {
                itemFlags.setMap(null);
            }
            else
            {
                split = value.toLowerCase().split("\\|");
                
                if(split.length == 0)
                {
                    recipeError("Flag @" + flag + " doesn't have any arguments!", null);
                    return;
                }
                
                for(String s : split)
                {
                    s = s.trim();
                    
                    if(s.equals("splash"))
                    {
                    }
                }
            }
            
            return;
        }
        
        /* TODO maybe - needs NMS/CB code...
        if(flag.equals("meta") || flag.equals("nbt"))
        {
            return;
        }
        */
        
        recipeError("Unknown flag: " + line, "Flag name might be diferent, check " + Files.FILE_INFO_FLAGS);
    }
    
    private Potion parsePotion(String value, String flag)
    {
        String[] split = value.toLowerCase().split("\\|");
        
        if(split.length == 0)
        {
            recipeError("Flag @" + flag + " doesn't have any arguments!", "It must have at least 'type' argument, read '" + Files.FILE_INFO_NAMES + "' for potion types list.");
            return null;
        }
        
        Potion potion = new Potion(null);
        boolean splash = false;
        boolean extended = false;
        int level = 1;
        
        for(String s : split)
        {
            s = s.trim();
            
            if(s.equals("splash"))
            {
                splash = true;
            }
            else if(s.equals("extended"))
            {
                extended = true;
            }
            else if(s.startsWith("type"))
            {
                split = s.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has 'type' argument with no type!", "Read '" + Files.FILE_INFO_NAMES + "' for potion types.");
                    return null;
                }
                
                value = split[1].trim();
                
                try
                {
                    potion.setType(PotionType.valueOf(value.toUpperCase()));
                }
                catch(Exception e)
                {
                    recipeError("Flag @" + flag + " has invalid 'type' argument value: " + value, "Read '" + Files.FILE_INFO_NAMES + "' for potion types.");
                    return null;
                }
            }
            else if(s.startsWith("level"))
            {
                split = s.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has 'level' argument with no level!", null);
                    continue;
                }
                
                value = split[1].trim();
                
                if(value.equals("max"))
                {
                    level = 9999;
                }
                else
                {
                    try
                    {
                        level = Integer.valueOf(value);
                    }
                    catch(Exception e)
                    {
                        recipeError("Flag @" + flag + " has invalid 'level' number: " + value, null);
                    }
                }
            }
            else
            {
                recipeError("Flag @" + flag + " has unknown argument: " + s, "Maybe it's spelled wrong, check it in " + Files.FILE_INFO_FLAGS + " file.");
            }
        }
        
        if(potion.getType() == null)
        {
            recipeError("Flag @" + flag + " is missing 'type' argument !", "Read '" + Files.FILE_INFO_NAMES + "' for potion types.");
            return null;
        }
        
        potion.setLevel(Math.min(Math.max(level, 1), potion.getType().getMaxLevel()));
        
        if(!potion.getType().isInstant())
            potion.setHasExtendedDuration(extended);
        
        potion.setSplash(splash);
        
        System.out.print("[debug] potion = " + potion.getLevel() + " | " + potion.getType() + " | " + potion.getEffects() + " | " + potion.isSplash() + " | " + potion.hasExtendedDuration());
        
        return potion;
    }
    
    private PotionEffect parsePotionEffect(String value, String flag)
    {
        String[] split = value.toLowerCase().split("\\|");
        
        if(split.length == 0)
        {
            recipeError("Flag @" + flag + " doesn't have any arguments!", "It must have at least 'type' argument, read '" + Files.FILE_INFO_NAMES + "' for potion effect types list.");
            return null;
        }
        
        PotionEffectType type = null;
        float duration = 1;
        int amplify = 0;
        boolean ambient = false;
        
        for(String s : split)
        {
            s = s.trim();
            
            if(s.equals("ambient"))
            {
                ambient = true;
            }
            else if(s.startsWith("type"))
            {
                split = s.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has 'type' argument with no type!", "Read '" + Files.FILE_INFO_NAMES + "' for potion effect types.");
                    return null;
                }
                
                value = split[1].trim();
                
                try
                {
                    type = PotionEffectType.getByName(value.toUpperCase());
                }
                catch(Exception e)
                {
                    recipeError("Flag @" + flag + " has invalid 'type' argument value: " + value, "Read '" + Files.FILE_INFO_NAMES + "' for potion effect types.");
                    return null;
                }
            }
            else if(s.startsWith("duration"))
            {
                split = s.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has 'duration' argument with no number!", null);
                    continue;
                }
                
                value = split[1].trim();
                
                try
                {
                    duration = Float.valueOf(value);
                }
                catch(Exception e)
                {
                    recipeError("Flag @" + flag + " has invalid 'duration' number: " + value, null);
                }
            }
            else if(s.startsWith("amplify"))
            {
                split = s.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has 'amplify' argument with no number!", null);
                    continue;
                }
                
                value = split[1].trim();
                
                try
                {
                    amplify = Integer.parseInt(value);
                }
                catch(Exception e)
                {
                    recipeError("Flag @" + flag + " has invalid 'amplify' number: " + value, null);
                }
            }
            else
            {
                recipeError("Flag @" + flag + " has unknown argument: " + s, "Maybe it's spelled wrong, check it in " + Files.FILE_INFO_FLAGS + " file.");
            }
        }
        
        if(type == null)
        {
            recipeError("Flag @" + flag + " is missing 'type' argument !", "Read '" + Files.FILE_INFO_NAMES + "' for potion effect types.");
            return null;
        }
        
        if(duration != 1 && (type == PotionEffectType.HEAL || type == PotionEffectType.HARM))
            recipeError("Flag @" + flag + " can't have duration on HEAL or HARM because they're instant! Ignored.", null);
        
        return new PotionEffect(type, Math.round(duration * 20), amplify, ambient);
    }
    
    private FireworkEffect parseFireworkEffect(String value, String flag)
    {
        String[] split = value.toLowerCase().split("\\|");
        
        if(split.length == 0)
        {
            recipeError("Flag @" + flag + " doesn't have any arguments!", "It must have at least one 'color' argument, read '" + Files.FILE_INFO_FLAGS + "' for syntax.");
            return null;
        }
        
        Builder build = FireworkEffect.builder();
        
        for(String s : split)
        {
            s = s.trim();
            
            if(s.equals("trail"))
            {
                build.withTrail();
            }
            else if(s.equals("flicker"))
            {
                build.withFlicker();
            }
            else if(s.startsWith("color"))
            {
                split = s.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has 'color' argument with no colors!", "Add colors separated by , in RGB format (3 numbers ranged 0-255)");
                    return null;
                }
                
                split = split[1].split(",");
                List<Color> colors = new ArrayList<Color>();
                Color color;
                
                for(String c : split)
                {
                    color = Tools.parseColor(c.trim());
                    
                    if(color == null)
                        recipeError("Flag @" + flag + " has an invalid color! Moving on...", null);
                    else
                        colors.add(color);
                }
                
                if(colors.isEmpty())
                {
                    recipeError("Flag @" + flag + " doesn't have any valid colors, they are required!", null);
                    return null;
                }
                
                build.withColor(colors);
            }
            else if(s.startsWith("fadecolor"))
            {
                split = s.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has 'fadecolor' argument with no colors!", "Add colors separated by , in RGB format (3 numbers ranged 0-255)");
                    return null;
                }
                
                split = split[1].split(",");
                List<Color> colors = new ArrayList<Color>();
                Color color;
                
                for(String c : split)
                {
                    color = Tools.parseColor(c.trim());
                    
                    if(color == null)
                        recipeError("Flag @" + flag + " has an invalid fade color! Moving on...", null);
                    else
                        colors.add(color);
                }
                
                if(colors.isEmpty())
                    recipeError("Flag @" + flag + " doesn't have any valid fade colors! Moving on...", null);
                else
                    build.withFade(colors);
            }
            else if(s.startsWith("type"))
            {
                split = s.split(" ", 2);
                
                if(split.length <= 1)
                {
                    recipeError("Flag @" + flag + " has 'type' argument with no value!", "Read " + Files.FILE_INFO_NAMES + " for list of firework effect types.");
                    return null;
                }
                
                value = split[1].trim();
                
                try
                {
                    build.with(FireworkEffect.Type.valueOf(value.toUpperCase()));
                }
                catch(Exception e)
                {
                    recipeError("Flag @" + flag + " has invalid 'type' setting value: " + value, "Read " + Files.FILE_INFO_NAMES + " for list of firework effect types.");
                    return null;
                }
            }
            else
            {
                recipeError("Flag @" + flag + " has unknown argument: " + s, "Maybe it's spelled wrong, check it in " + Files.FILE_INFO_FLAGS + " file.");
            }
        }
        
        return build.build();
    }
    
    private String[] parseCraftRecipe() throws Exception
    {
        debug("parsing craft recipe...");
        
        CraftRecipe recipe = new CraftRecipe(fileFlags); // create recipe and copy flags from file
        parseFlags(recipe.getFlags(), RecipeType.CRAFT, null); // parse recipe's flags
        
        ItemStack[] ingredients = new ItemStack[9];
        String split[];
        ItemStack item;
        int rows = 0;
        boolean ingredientErrors = false;
        
        while(rows < 3) // loop until we find 3 rows of ingredients (or bump into the result along the way)
        {
            if(rows > 0)
                nextLine();
            
            debug("searching for ingredients...");
            
            if(line.charAt(0) == '=') // if we bump into the result prematurely (smaller recipes)
                break;
            
            split = line.split("\\+"); // split ingredients by the + sign
            int rowLen = split.length;
            
            if(rowLen > 3) // if we find more than 3 ingredients warn the user and limit it to 3
            {
                rowLen = 3;
                recipeError("You can't have more than 3 ingredients on a row, ingredient(s) ignored.", "Remove the extra ingredient(s).");
            }
            
            for(int i = 0; i < rowLen; i++) // go through each ingredient on the line
            {
                if((item = convertStringToItemStack(split[i], -1, true, false, false, true)) == null) // invalid item
                    ingredientErrors = true;
                
                if(ingredientErrors) // no point in adding more ingredients if there are errors
                    continue;
                
                if(item.getTypeId() != 0)
                    ingredients[(rows * 3) + i] = item;
            }
            
            rows++;
        }
        
        if(ingredientErrors) // invalid ingredients found
            return new String[] { "Recipe has some invalid ingredients, fix them!" };
        else if(rows == 0) // no ingredients were processed
            return new String[] { "Recipe doesn't have ingredients !", "Consult readme.txt for proper recipe syntax." };
        
        recipe.setIngredients(ingredients); // done with ingredients, set'em
        debug("set ingredients...");
        
        // get results
        List<ItemResult> results = new ArrayList<ItemResult>();
        String[] errors = parseResults(recipe, results, false, false);
        
        if(errors != null) // results have errors
            return errors;
        
        recipe.setResults(results); // done with results, set'em
        
        // check if the recipe already exists...
        errors = recipeCheckExists(recipe);
        
        if(errors != null)
            return errors;
        
        debug("done with recipe...");
        
        // add the recipe to the Recipes class and to the list for later adding to the server
        registrator.queueCraftRecipe(recipe, currentFile);
        loaded++;
        return null; // no errors encountered
    }
    
    private String[] parseCombineRecipe() throws Exception
    {
        debug("parsing combine recipe...");
        
        CombineRecipe recipe = new CombineRecipe(fileFlags); // create recipe and copy flags from file
        parseFlags(recipe.getFlags(), RecipeType.COMBINE, null); // parse recipe's flags
        
        // get the ingredients
        String[] ingredientsRaw = line.split("\\+");
        
        List<ItemStack> ingredients = new ArrayList<ItemStack>();
        ItemStack item;
        int items = 0;
        
        for(String str : ingredientsRaw)
        {
            item = convertStringToItemStack(str, -1, true, true, false, true);
            
            if(item == null)
                return new String[] { "Recipe has some invalid ingredients, fix them!" };
            
            if((items += item.getAmount()) > 9)
                return new String[] { "Combine recipes can't have more than 9 ingredients !", "If you're using stacks make sure they don't exceed 9 items in total." };
            
            ingredients.add(item);
        }
        
        recipe.setIngredients(ingredients);
        
        // get the results
        List<ItemResult> results = new ArrayList<ItemResult>();
        String[] resultErrors = parseResults(recipe, results, false, false);
        
        if(resultErrors != null)
            return resultErrors;
        
        recipe.setResults(results);
        
        // check if recipe already exists
        String[] errors = recipeCheckExists(recipe);
        
        if(errors != null)
            return errors;
        
        // add the recipe to the Recipes class and to the list for later adding to the server
        registrator.queueCombineRecipe(recipe, currentFile);
        loaded++;
        return null; // no errors encountered
    }
    
    private String[] parseSmeltRecipe() throws Exception
    {
        debug("parsing smelting recipe...");
        
        SmeltRecipe recipe = new SmeltRecipe(fileFlags); // create recipe and copy flags from file
        parseFlags(recipe.getFlags(), RecipeType.SMELT, null); // check for @flags
        
        // get the ingredient and smelting time
        String[] split = line.split("%");
        
        if(split.length == 0)
            return new String[] { "Smeling recipe doesn't have an ingredient !" };
        
        ItemStack ingredient = convertStringToItemStack(split[0], -1, true, false, false, true);
        
        if(ingredient == null)
            return new String[] { "Invalid ingredient '" + split[0] + "'.", "Name could be diferent, look in readme.txt for links." };
        
        recipe.setIngredient(ingredient);
        
        // get min-max or fixed smelting time
        if(!recipe.getFlags().isRemove()) // if it's got @remove we don't care about burn time
        {
            float minTime = -1;
            float maxTime = -1;
            
            if(split.length >= 2)
            {
                String[] timeSplit = split[1].trim().split("-");
                
                if(!timeSplit[0].equals("INSTANT"))
                {
                    try
                    {
                        minTime = Float.valueOf(timeSplit[0]);
                        
                        if(timeSplit.length >= 2)
                            maxTime = Float.valueOf(timeSplit[1]);
                    }
                    catch(NumberFormatException e)
                    {
                        recipeError("Invalid burn time float number! Smelt time set to default.", null);
                        minTime = -1;
                        maxTime = -1;
                    }
                }
                else
                    minTime = 0;
                
                if(maxTime > -1.0 && minTime >= maxTime)
                    return new String[] { "Smelting recipe has the min-time less or equal to max-time!", "Use a single number if you want a fixed value." };
            }
            
            recipe.setMinTime(minTime);
            recipe.setMaxTime(maxTime);
        }
        
        // get result or move current line after them if we got @remove and results
        List<ItemResult> results = new ArrayList<ItemResult>();
        String[] resultErrors = parseResults(recipe, results, false, true);
        
        if(!recipe.getFlags().isRemove()) // ignore results and results errors if we have @remove
        {
            if(resultErrors != null)
                return resultErrors;
            
            if(results.size() > 1)
                recipeError("Can't have more than 1 result in smelting recipes! Rest of results ignored.", null);
            
            recipe.setResult(results.get(0));
        }
        
        // check if the recipe already exists
        String[] errors = recipeCheckExists(recipe);
        
        if(errors != null)
            return errors;
        
        // add the recipe to the Recipes class and to the list for later adding to the server
        registrator.queueSmeltRecipe(recipe, currentFile);
        loaded++;
        return null;
    }
    
    private String[] parseFuelRecipe() throws Exception
    {
        debug("parsing fuel recipe...");
        
        FuelRecipe recipe = new FuelRecipe(fileFlags); // create recipe and copy flags from file
        parseFlags(recipe.getFlags(), RecipeType.FUEL, null); // check for @flags
        
        // get the ingredient and burning time
        String[] split = line.split("%");
        
        if(!recipe.getFlags().isRemove()) // if it's got @remove we don't care about burn time
        {
            if(split.length < 2 || split[1] == null)
                return new String[] { "Burn time not set !", "It must be set after the ingredient like: ingredient % burntime" };
            
            // set the burn time
            String[] timeSplit = split[1].trim().split("-");
            float minTime = -1;
            float maxTime = -1;
            
            try
            {
                minTime = Math.max(Float.valueOf(timeSplit[0]), 1);
                
                if(timeSplit.length >= 2)
                    maxTime = (float)Math.max(Float.valueOf(timeSplit[1]), 0.0);
            }
            catch(NumberFormatException e)
            {
                return new String[] { "Invalid burn time float number!" };
            }
            
            if(minTime <= 0)
                return new String[] { "Fuels can't burn for negative or 0 seconds!" };
            
            if(maxTime > -1 && minTime >= maxTime)
                return new String[] { "Fuel has minimum time less or equal to maximum time!", "Use a single number if you want a fixed value" };
            
            recipe.setMinTime(minTime);
            recipe.setMaxTime(maxTime);
        }
        
        // set ingredient
        ItemStack ingredient = convertStringToItemStack(split[0], -1, true, false, false, true);
        
        if(ingredient == null || ingredient.getTypeId() == 0)
            return new String[] { "Invalid item: '" + ingredient + "'" };
        
        recipe.setIngredient(ingredient);
        
        // check if the recipe already exists
        String[] errors = recipeCheckExists(recipe);
        
        if(errors != null)
            return errors;
        
        registrator.queuFuelRecipe(recipe, currentFile);
        
        debug("done with fuel !");
        
        loaded++;
        return null;
    }
    
    private String[] parseRemoveResult() throws Exception
    {
        ItemStack item = convertStringToItemStack(line, 1, true, true, true, true);
        
        if(item == null)
            return new String[] { "Invalid item!" };
        
        loaded++;
        return null;
    }
    
    private String[] parseResults(BaseRecipe recipe, List<ItemResult> results, boolean allowAir, boolean oneResult) throws Exception
    {
        int totalpercentage = 0;
        ItemResult resultCalc = null;
        ItemResult result;
        
        if(line.charAt(0) != '=') // check if current line is a result, if not move on
            nextLine();
        
        while(line != null && line.charAt(0) == '=')
        {
            result = convertStringToItemResult(line, 0, true, true, true, true); // convert result to ItemResult, grabbing chance and whatother stuff
            
            if(result == null || (!allowAir && result.getTypeId() == 0))
                return new String[] { "Invalid result !", "Result might be missing or just be incorectly typed, see previous errors if any." };
            
            if((totalpercentage += result.getChance()) > 100)
                return new String[] { "Total result items' chance exceeds 100% !", "Not defining percentage for one item will make its chance fit with the rest!" };
            
            if(result.getChance() == -1) // check if result has a specific chance set
            {
                if(resultCalc != null)
                    return new String[] { "Can't have more than 1 item without percentage to fill the rest!" };
                
                resultCalc = result;
                parseFlags(resultCalc.getFlags(), null, resultCalc); // check for result flags and keeps the line flow going too
            }
            else
            {
                results.add(result);
                parseFlags(result.getFlags(), null, result); // check for result flags and keeps the line flow going too                
            }
        }
        
        if(resultCalc != null)
        {
            resultCalc.setChance(100 - totalpercentage);
            results.add(resultCalc);
        }
        
        else if(results.isEmpty())
            return new String[] { "Found '=' character but without result item !" };
        
        else if(!oneResult && totalpercentage < 100)
            results.add(new ItemResult(Material.AIR, 0, 0, (100 - totalpercentage)));
        
        debug("done with results...");
        
        return null;
    }
    
    private ItemResult convertStringToItemResult(String string, int defaultData, boolean allowData, boolean allowAmount, boolean allowEnchantments, boolean printErrors)
    {
        String[] split = string.substring(1).trim().split("%");
        ItemResult result = new ItemResult();
        
        if(split.length >= 2)
        {
            string = split[0].trim();
            
            try
            {
                result.setChance(Math.min(Math.max(Integer.valueOf(string), 0), 100));
            }
            catch(Exception e)
            {
                recipeError("Invalid percentage number: " + string, null);
            }
            
            string = split[1];
        }
        else
            string = split[0];
        
        ItemStack item = convertStringToItemStack(string, defaultData, allowData, allowAmount, allowEnchantments, printErrors);
        
        if(item == null)
            return null;
        
        result.setItemStack(item);
        
        return result;
    }
    
    private ItemStack convertStringToItemStack(String string, int defaultData, boolean allowData, boolean allowAmount, boolean allowEnchantments, boolean printErrors)
    {
        string = string.trim();
        
        if(string.length() == 0)
            return null;
        
        String[] itemString = string.split("\\|");
        String[] stringArray = itemString[0].trim().split(":");
        
        if(stringArray.length <= 0 || stringArray[0].isEmpty())
            return new ItemStack(0);
        
        stringArray[0] = stringArray[0].trim();
        
        /*
        String alias = RecipeManager.getPlugin().getAliases().get(stringArray[0]);
        
        if(alias != null)
        {
            if(stringArray.length > 2 && printErrors)
                recipeError("'" + stringArray[0] + "' is an alias with data and amount.", "You can only set amount e.g.: alias:amount.");
            
            return stringToItemStack(string.replace(stringArray[0], alias), defaultData, allowData, allowAmount, allowEnchantments, printErrors);
        }
        */
        
        Material mat = Material.matchMaterial(stringArray[0]);
        
        if(mat == null)
        {
            if(printErrors)
                recipeError("Item '" + stringArray[0] + "' does not exist!", "Name could be different, look in readme.txt for links");
            
            return null;
        }
        
        int type = mat.getId();
        
        if(type <= 0)
            return new ItemStack(0);
        
        int data = defaultData;
        
        if(stringArray.length > 1)
        {
            if(allowData)
            {
                // TODO maybe use TreeSpecies, SkullTypes, etc as data aliases ?
                
                try
                {
                    stringArray[1] = stringArray[1].trim();
                    
                    if(stringArray[1].charAt(0) != '*')
                        data = Math.max(Integer.valueOf(stringArray[1]), data);
                }
                catch(Exception e)
                {
                    if(printErrors)
                        recipeError("Item '" + mat + " has data value that is not a number: '" + stringArray[1] + "', defaulting to " + defaultData, null);
                }
            }
            else if(printErrors)
                recipeError("Item '" + mat + "' can't have data value defined in this recipe's slot, data value ignored.", null);
        }
        
        int amount = 1;
        
        if(stringArray.length > 2)
        {
            if(allowAmount)
            {
                try
                {
                    amount = Math.max(Integer.valueOf(stringArray[2].trim()), 1);
                }
                catch(Exception e)
                {
                    if(printErrors)
                        recipeError("Item '" + mat + "' has amount value that is not a number: " + stringArray[2] + ", defaulting to 1", null);
                }
            }
            else if(printErrors)
                recipeError("Item '" + mat + "' can't have amount defined in this recipe's slot, amount ignored.", null);
        }
        
        ItemStack item = new ItemStack(type, amount, (short)data);
        
        if(itemString.length > 1)
        {
            if(allowEnchantments)
            {
                if(item.getAmount() > 1)
                {
                    if(printErrors)
                        recipeError("Item '" + mat + "' has enchantments and more than 1 amount, it can't have both, amount set to 1.", null);
                    
                    item.setAmount(1);
                }
                
                String[] enchants = itemString[1].split(",");
                String[] enchData;
                Enchantment ench;
                int level;
                
                for(String enchant : enchants)
                {
                    enchant = enchant.trim();
                    enchData = enchant.split(":");
                    
                    if(enchData.length != 2)
                    {
                        if(printErrors)
                            recipeError("Enchantments have to be 'ENCHANTMENT:LEVEL' format.", "Look in readme.txt for enchantment list link.");
                        
                        continue;
                    }
                    
                    ench = Enchantment.getByName(enchData[0]);
                    
                    if(ench == null)
                    {
                        try
                        {
                            ench = Enchantment.getById(Integer.valueOf(enchData[0]));
                        }
                        catch(Exception e)
                        {
                            ench = null;
                        }
                        
                        if(ench == null)
                        {
                            if(printErrors)
                                recipeError("Enchantment '" + enchData[0] + "' does not exist!", "Name or ID could be different, look in readme.txt for enchantments list links.");
                            
                            continue;
                        }
                    }
                    
                    if(enchData[1].equals("MAX"))
                        level = ench.getMaxLevel();
                    
                    else
                    {
                        try
                        {
                            level = Integer.valueOf(enchData[1]);
                        }
                        catch(Exception e)
                        {
                            if(printErrors)
                                recipeError("Invalid enchantment level: '" + enchData[1] + "' must be a valid number, positive, zero or negative.", null);
                            
                            continue;
                        }
                    }
                    
                    item.addUnsafeEnchantment(ench, level);
                }
            }
            else if(printErrors)
                recipeError("Item '" + mat + "' can't use enchantments in this recipe slot!", null);
        }
        
        return item;
    }
    
    private String[] recipeCheckExists(BaseRecipe recipe) // TODO
    {
        RecipeInfo registered = RecipeManager.recipes.index.get(recipe);
        
        if(recipe.getFlags().isOverride() || recipe.getFlags().isRemove())
        {
            if(registered == null)
            {
                return new String[] { "Recipe was not found, can't override/remove it! Added as new recipe.", "Use 'rmextract' command to see the exact ingredients needed" };
            }
            else if(registered.getOwner() == RecipeOwner.RECIPEMANAGER && registered.getStatus() == null)
            {
                return new String[] { "Can't override/remove RecipeManager's recipes - just edit the recipe files!" };
            }
            
            return null;
        }
        
        if(registered != null && registered.getOwner() == RecipeOwner.RECIPEMANAGER && !currentFile.equals(registered.getAdder()))
        {
            return new String[] { "Recipe already created with this plugin, file: " + registered.getAdder() };
        }
        
        RecipeInfo queued = registrator.queuedRecipes.get(recipe);
        
        if(queued != null)
        {
            return new String[] { "Recipe already created with this plugin, file: " + queued.getAdder() };
        }
        
        /*
        RecipeInfo infoExisting = RecipeManager.getRecipes() == null ? null : RecipeManager.getRecipes().getRecipeInfo(recipe);
        RecipeInfo infoProcessed = registrator.queuedRecipes.get(recipe);
        boolean override = recipe.getFlags().isOverride();
        boolean remove = recipe.getFlags().isRemove();
        
        if(infoProcessed == null && infoExisting == null)
        {
            if(remove || override)
            {
                return new String[] { "Recipe was not found, can't " + (override ? "override" : "remove") + " it! Added as new recipe.", "Use 'rmextract' command to see the exact ingredients needed" };
            }
            
            return null;
        }
        
        if((infoProcessed != null && infoProcessed.getOwner() == RecipeOwner.RECIPEMANAGER) || (infoExisting != null && infoExisting.getOwner() == RecipeOwner.RECIPEMANAGER))
        {
            return new String[] { "Recipe already created with this plugin, file: " + (infoProcessed == null ? infoExisting.getAdder() : infoProcessed.getAdder()), (override || remove ? "You can't @override or @remove recipes that you added using this plugin because you can just edit or delete them from the files." : null) };
        }
        else if(!(remove || override))
        {
            return new String[] { "Recipe already exists, added by " + (infoProcessed == null ? infoExisting.getAdder() : infoProcessed.getAdder()), "You can use @override flag to change recipe's result(s)." };
        }
        */
        
        return null;
    }
    
    private void recipeError(String error, String tip)
    {
        List<String> errors = recipeErrors.get(currentFile);
        
        if(errors == null)
            errors = new ArrayList<String>();
        
        errors.add("line " + String.format("%-5d", lineNum) + ChatColor.WHITE + error + (tip != null ? NL + ChatColor.DARK_GREEN + "          TIP: " + ChatColor.GRAY + tip : ""));
        
        recipeErrors.put(currentFile, errors);
    }
}