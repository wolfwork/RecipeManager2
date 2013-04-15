package ro.thehunters.digi.recipeManager.flags;

import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.FireworkEffectMeta;

import ro.thehunters.digi.recipeManager.RecipeErrorReporter;
import ro.thehunters.digi.recipeManager.Tools;
import ro.thehunters.digi.recipeManager.recipes.ItemResult;

public class FlagItemFireworkCharge extends Flag
{
    // Flag documentation
    
    public static final String[] A;
    public static final String[] D;
    public static final String[] E;
    
    static
    {
        A = new String[1];
        A[0] = "{flag} < ??? >";
        
        D = new String[1];
        D[0] = "Flag not yet documented.";
        
        E = null;
    }
    
    // Flag code
    
    public FlagItemFireworkCharge()
    {
        type = FlagType.ITEMFIREWORKCHARGE;
    }
    
    @Override
    public boolean onValidate()
    {
        ItemResult result = getResult();
        
        if(result == null || result.getItemMeta() instanceof FireworkEffectMeta == false)
        {
            RecipeErrorReporter.error("Flag " + type + " needs a FIREWORK_CHARGE item!");
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean onParse(String value)
    {
        FireworkEffect effect = Tools.parseFireworkEffect(value, type);
        
        if(effect != null)
        {
            ItemResult result = getResult();
            FireworkEffectMeta firework = (FireworkEffectMeta)result.getItemMeta();
            firework.setEffect(effect);
            result.setItemMeta(firework);
        }
        
        return true;
    }
}