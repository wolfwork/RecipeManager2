package ro.thehunters.digi.recipeManager.flags;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

public class FlagMessage extends Flag
{
    // Flag definition and documentation
    
    private static final FlagType TYPE;
    protected static final String[] A;
    protected static final String[] D;
    protected static final String[] E;
    
    static
    {
        TYPE = FlagType.MESSAGE;
        
        A = new String[]
        {
            "{flag} <text>",
        };
        
        D = new String[]
        {
            "Prints a message when recipe or item is succesfully crafted.",
            "This flag can be used more than once to add more messages.",
            "The text can contain colors (<red>, &5, etc)",
        };
        
        E = new String[]
        {
            "{flag} <green>Good job !",
            "{flag} <gray>Now you can die&c happy<gray> that you crafted that.",
        };
    }
    
    // Flag code
    
    private List<String> messages = new ArrayList<String>();
    
    public FlagMessage()
    {
    }
    
    public FlagMessage(FlagMessage flag)
    {
        messages.addAll(flag.messages);
    }
    
    @Override
    public FlagMessage clone()
    {
        return new FlagMessage(this);
    }
    
    @Override
    public FlagType getType()
    {
        return TYPE;
    }
    
    public List<String> getMessages()
    {
        return messages;
    }
    
    /**
     * Set the message list.
     * 
     * @param messages
     */
    public void setMessages(List<String> messages)
    {
        if(messages == null)
        {
            this.remove();
        }
        else
        {
            this.messages = messages;
        }
    }
    
    /**
     * Set the message.<br>
     * Supports parsable color tags and codes.<br>
     * You can use null, "false" or "remove" to remove the entire flag.
     * 
     * @param message
     */
    public void addMessage(String message)
    {
        if(message == null || message.equalsIgnoreCase("false") || message.equalsIgnoreCase("remove"))
        {
            this.remove();
        }
        else
        {
            if(messages == null)
            {
                messages = new ArrayList<String>();
            }
            
            messages.add(message);
        }
    }
    
    @Override
    protected boolean onParse(String value)
    {
        addMessage(value);
        return true;
    }
    
    @Override
    protected void onCrafted(Args a)
    {
        Validate.notNull(messages);
        
        for(String s : messages)
        {
            a.addCustomEffect(a.parseVariables(s));
        }
    }
}
