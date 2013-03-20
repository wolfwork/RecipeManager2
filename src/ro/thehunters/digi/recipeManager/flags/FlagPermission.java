package ro.thehunters.digi.recipeManager.flags;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import ro.thehunters.digi.recipeManager.Messages;
import ro.thehunters.digi.recipeManager.Tools;

public class FlagPermission extends Flag
{
    private List<String> permissions     = new ArrayList<String>();
    private List<String> antiPermissions = new ArrayList<String>();
    private String       message;
    
    public FlagPermission()
    {
        type = FlagType.PERMISSION;
    }
    
    @Override
    public FlagPermission clone()
    {
        FlagPermission clone = new FlagPermission();
        
        clone.permissions.addAll(permissions);
        clone.message = message;
        
        return clone;
    }
    
    public List<String> getPermissions()
    {
        return permissions;
    }
    
    public void setPermissions(List<String> permissions)
    {
        this.permissions = permissions;
    }
    
    public void addPermission(String permission)
    {
        permissions.add(permission);
    }
    
    public List<String> getAntiPermissions()
    {
        return antiPermissions;
    }
    
    public void setAntiPermissions(List<String> antiPermissions)
    {
        this.antiPermissions = antiPermissions;
    }
    
    public void addAntiPermission(String permission)
    {
        antiPermissions.add(permission);
    }
    
    public String getMessage()
    {
        return message;
    }
    
    public void setMessage(String message)
    {
        this.message = message;
    }
    
    @Override
    public boolean onParse(String value)
    {
        String[] split = value.split("\\|");
        
        if(split.length > 1)
        {
            setMessage(split[1].trim());
        }
        
        value = split[0].trim();
        
        if(value.charAt(0) == '-')
        {
            addAntiPermission(value.substring(1).trim());
        }
        else
        {
            addPermission(value);
        }
        
        return true;
    }
    
    @Override
    public void onCheck(Args a)
    {
        if(!a.hasPlayer())
        {
            if(!permissions.isEmpty())
            {
                a.addReason(Messages.FLAG_PERMISSION_NEED, message, "{permission}", permissions.get(0), "{permissions}", Tools.listToString(permissions));
            }
            
            return;
        }
        
        Player player = a.player();
        boolean ok = false;
        
        for(String perm : permissions)
        {
            if(player.hasPermission(perm))
            {
                ok = true;
                break;
            }
        }
        
        if(!ok)
        {
            a.addReason(Messages.FLAG_PERMISSION_NEED, message, "{permission}", permissions.get(0), "{permissions}", Tools.listToString(permissions));
        }
        
        for(String perm : antiPermissions)
        {
            if(player.hasPermission(perm))
            {
                a.addReason(Messages.FLAG_PERMISSION_UNALLOWED, message, "{permission}", perm, "{permissions}", Tools.listToString(antiPermissions));
                break;
            }
        }
    }
}