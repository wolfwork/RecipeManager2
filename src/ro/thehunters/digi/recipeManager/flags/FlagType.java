package ro.thehunters.digi.recipeManager.flags;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import ro.thehunters.digi.recipeManager.Messages;
import ro.thehunters.digi.recipeManager.Permissions;

public enum FlagType
{
    // TODO go through each flag and check:
    // new Flag(this) clone style and must have this() in them !!!!!
    // protected on*() methods
    // ...
    
    // Shared flags
    MESSAGE(FlagMessage.class, Bit.NONE, "craftmsg"),
    COMMANDS(FlagCommands.class, Bit.NONE, "command", "cmd"),
    PERMISSION(FlagPermission.class, Bit.NONE, "perm"),
    FORPERMISSION(FlagForPermission.class, Bit.NO_SHIFT, "forperm", "for"),
    INGREDIENTCONDITION(FlagIngredientCondition.class, Bit.NO_SHIFT, "ingredient", "ifingr"), // TODO finish
//    HOLDITEM(FlagHoldItem.class, Bit.NONE, "hold"),
//    PLAYTIME(FlagPlayTime.class, Bit.NONE),
//    ONLINETIME(FlagOnlineTime.class, Bit.NONE),
//    GAMEMODE(FlagGameMode.class, Bit.NONE),
    MODEXP(FlagModExp.class, Bit.NO_SHIFT, "expmod", "modxp", "xpmod", "exp", "xp", "giveexp", "givexp", "takeexp", "takexp"),
    REQEXP(FlagReqExp.class, Bit.NONE, "expreq", "reqxp", "xpreq", "needexp", "needxp"),
    MODLEVEL(FlagModLevel.class, Bit.NO_SHIFT, "levelmod", "level"),
    REQLEVEL(FlagReqLevel.class, Bit.NONE, "levelreq", "needlevel"),
    MODMONEY(FlagModMoney.class, Bit.NO_SHIFT, "moneymod", "money"),
    REQMONEY(FlagReqMoney.class, Bit.NONE, "moneyreq", "needmoney"),
    LAUNCHFIREWORK(FlagLaunchFirework.class, Bit.NO_SHIFT),
    EXPLODE(FlagExplode.class, Bit.NO_SHIFT, "explosion", "boom"),
    SOUND(FlagSound.class, Bit.NO_SHIFT, "playsound"),
    EFFECT(FlagEffect.class, Bit.NO_SHIFT, "playeffect", "fx"), // TODO finish
    CREATURE(FlagCreature.class, Bit.NO_SHIFT, "spawncreature"), // TODO finish
    BIOME(FlagBiome.class, Bit.NONE), // TODO finish
    WEATHER(FlagWeather.class, Bit.NONE), // TODO finish
    WORLDTIME(FlagWorldTime.class, Bit.NONE), // TODO finish
    SECRET(FlagSecret.class, Bit.NO_VALUE, "hide"),
    DEBUG(FlagDebug.class, Bit.NO_VALUE, "monitor", "log"),
    REALTIME(FlagRealTime.class, Bit.NONE, "time", "timereq"),
    COOLDOWN(FlagCooldown.class, Bit.NO_SHIFT, "cooltime", "delay"),
    RETURNITEM(FlagReturnItem.class, Bit.NO_SHIFT, "returningr", "returningredient"),
    
    // Recipe only flags
    DESCRIPTION(FlagDescription.class, Bit.RECIPE, "recipeinfo", "info"),
    FAILMESSAGE(FlagFailMessage.class, Bit.RECIPE, "failmsg"),
    HIDERESULTS(FlagHideResults.class, Bit.RECIPE | Bit.NO_VALUE),
    GETBOOK(FlagGetBook.class, Bit.RECIPE | Bit.NO_SHIFT, "getrecipebook", "recipebook"), // TODO finsih
    REMOVE(FlagRemove.class, Bit.RECIPE | Bit.NO_VALUE, "delete"),
    RESTRICT(FlagRestrict.class, Bit.RECIPE | Bit.NO_VALUE, "denied", "deny"),
    OVERRIDE(FlagOverride.class, Bit.RECIPE | Bit.NO_VALUE, "edit", "overwrite", "supercede", "replace"),
//    PROXIMITY(FlagProximity.class, Bit.RECIPE, "distance", "nearby"), // TODO
    
    // Result only flags
//    SETCHANCE(FlagSetChance.class, Bit.RESULT, "chance"), // TODO finish
    CLONEINGREDIENT(FlagCloneIngredient.class, Bit.RESULT | Bit.NO_SHIFT, "clone", "copy", "copyingredient"), // TODO finish
    NAME(FlagName.class, Bit.RESULT | Bit.NO_STORE, "itemname", "displayname"),
    LORE(FlagLore.class, Bit.RESULT | Bit.NO_STORE, "itemlore", "itemdescription"),
    LEATHERCOLOR(FlagLeatherColor.class, Bit.RESULT | Bit.NO_STORE, "leathercolour", "color", "colour", "itemcolor", "itemcolour"),
    BOOK(FlagBook.class, Bit.RESULT | Bit.NO_STORE, "bookitem", "itembook"),
    BOOKPAGE(FlagBookPage.class, Bit.RESULT | Bit.NO_STORE, "bookitempage", "page", "addpage"),
    MAP(FlagMap.class, Bit.RESULT | Bit.NO_STORE, "mapitem", "itemmap"),
    FIREWORK(FlagFirework.class, Bit.RESULT | Bit.NO_STORE, "fireworkrocket"),
    FIREWORKCHARGE(FlagFireworkCharge.class, Bit.RESULT | Bit.NO_STORE, "fireworkeffect"),
    SKULL(FlagSkull.class, Bit.RESULT | Bit.NO_STORE, "skullowner"),
    POTION(FlagPotion.class, Bit.RESULT | Bit.NO_STORE, "potionitem"),
    ENCHANT(FlagEnchant.class, Bit.RESULT | Bit.NO_STORE, "enchantment"),
    ENCHANTBOOK(FlagEnchantBook.class, Bit.RESULT | Bit.NO_STORE, "enchantedbook");
    
    /*
     *  FlagType related methods
     */
    
    private final Class<? extends Flag> flagClass;
    private final String[]              names;
    private final int                   bits;
    
    private FlagType(Class<? extends Flag> flagClass, int bits, String... aliases)
    {
        this.flagClass = flagClass;
        this.bits = bits;
        
        this.names = new String[aliases.length + 1];
        this.names[0] = name().toLowerCase();
        
        for(int i = 0; i < aliases.length; i++)
        {
            this.names[i + 1] = aliases[i];
        }
    }
    
    public boolean hasBit(int bit)
    {
        return (bits & bit) == bit;
    }
    
    /**
     * @return the class asigned to this type (not the instance)
     */
    public Class<? extends Flag> getFlagClass()
    {
        return flagClass;
    }
    
    /**
     * @return array of flags names, index 0 is always the main name
     */
    public String[] getNames()
    {
        return names;
    }
    
    /**
     * @return the first name of the flag
     */
    public String getName()
    {
        return names[0];
    }
    
    /**
     * @return a new instance of the class asigned to this type or null if failed and prints stack trace.
     */
    public Flag createFlagClass()
    {
        try
        {
            return flagClass.newInstance();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Gets the <code>@flag</code> style flag name
     */
    public String toString()
    {
        return "@" + names[0];
    }
    
    /*
     *  Static stuff
     */
    
    private static final Map<String, FlagType>                nameMap  = new HashMap<String, FlagType>();
    private static final Map<Class<? extends Flag>, FlagType> classMap = new HashMap<Class<? extends Flag>, FlagType>();
    
    /**
     * You should not call this method.<br>
     * <br>
     * It is used by the plugin to add the flags to an index map then create and add individual no-flag permissions.
     */
    public static void init()
    {
        Permission parent = new Permission(Permissions.SKIPFLAG_ALL, PermissionDefault.FALSE);
        parent.setDescription("Ignores all flags.");
        Bukkit.getPluginManager().addPermission(parent);
        Permission p;
        
        for(FlagType type : values())
        {
            classMap.put(type.getFlagClass(), type);
            
            for(String name : type.names)
            {
                nameMap.put(name, type);
                
                if(!type.hasBit(Bit.NO_STORE))
                {
                    if(Bukkit.getPluginManager().getPermission(Permissions.SKIPFLAG_PREFIX + name) != null)
                    {
                        Messages.debug("permission for flag " + name + " already exists!");
                        continue;
                    }
                    
                    p = new Permission(Permissions.SKIPFLAG_PREFIX + name, PermissionDefault.FALSE);
                    p.setDescription("Ignores the " + type + " flag.");
                    p.addParent(parent, true);
                    Bukkit.getPluginManager().addPermission(p);
                }
            }
        }
    }
    
    /**
     * Get the FlagType object for a flag name or alias.
     * 
     * @param flag
     *            flag name or alias
     * @return FlagType if found or null
     */
    public static FlagType getByName(String flag)
    {
        Validate.notNull(flag);
        
        if(flag.charAt(0) != '@')
        {
            throw new IllegalArgumentException("Flag string must start with @");
        }
        
        return nameMap.get(flag.substring(1).toLowerCase());
    }
    
    /**
     * Get the FlagType object for the specified class.
     * 
     * @param flagClass
     *            flag's .class
     * @return FlagType if found or null
     */
    public static FlagType getByClass(Class<? extends Flag> flagClass)
    {
        return classMap.get(flagClass);
    }
    
    /**
     * Flag bits to configure special behaviour
     */
    public class Bit
    {
        public static final byte NONE     = 0;
        
        /**
         * Flag only works in recipes.
         */
        public static final byte RECIPE   = 1 << 1;
        
        /**
         * Flag only works on results.
         */
        public static final byte RESULT   = 1 << 2;
        
        /**
         * No value is allowed for this flag.
         */
        public static final byte NO_VALUE = 1 << 3;
        
        /**
         * Disables flag from being stored - used on flags that directly affect result's metadata.
         */
        public static final byte NO_STORE = 1 << 4;
        
        /**
         * Disables "false" or "remove" values from removing the flag.
         */
        public static final byte NO_FALSE = 1 << 5;
        
        /**
         * Disables shift+click on the recipe if there is at least one flag with this bit.
         */
        public static final byte NO_SHIFT = 1 << 6;
    }
}