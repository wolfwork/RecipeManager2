package ro.thehunters.digi.recipeManager.flags;

import java.util.Date;

import ro.thehunters.digi.recipeManager.Messages;

public class FlagRealTime extends Flag
{
    // Flag definition and documentation
    
    private static final FlagType TYPE;
    protected static final String[] A;
    protected static final String[] D;
    protected static final String[] E;
    
    static
    {
        TYPE = FlagType.REALTIME;
        
        A = null;
        D = null;
        E = null;
    }
    
    // Flag code
    
    private int minTime;
    private int maxTime;
    private String message;
    
    public FlagRealTime()
    {
    }
    
    public FlagRealTime(FlagRealTime flag)
    {
        this();
        
        minTime = flag.minTime;
        maxTime = flag.maxTime;
        message = flag.message;
    }
    
    @Override
    public FlagRealTime clone()
    {
        return new FlagRealTime(this);
    }
    
    @Override
    public FlagType getType()
    {
        return TYPE;
    }
    
    public int getMinTime()
    {
        return minTime;
    }
    
    public void setMinTime(int minTime)
    {
        this.minTime = minTime;
    }
    
    public String getMinDate()
    {
        return new Date(minTime * 1000).toString();
    }
    
    public int getMaxTime()
    {
        return maxTime;
    }
    
    public void setMaxTime(int maxTime)
    {
        this.maxTime = maxTime;
    }
    
    public String getMaxDate()
    {
        return new Date(maxTime * 1000).toString();
    }
    
    public boolean checkTime()
    {
        return checkTime((int)(System.currentTimeMillis() / 1000));
    }
    
    /**
     * @param currentTime
     *            time in seconds
     * @return
     */
    public boolean checkTime(int currentTime)
    {
        return (minTime < currentTime && maxTime > currentTime);
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
    protected boolean onParse(String value)
    {
        String[] split = value.split("\\|");
        
        if(split.length > 1)
        {
            setMessage(split[1].trim());
        }
        
        split = split[0].split("");
        
        // TODO
        /*
        try
        {
            exp = Integer.valueOf(value);
        }
        catch(NumberFormatException e)
        {
            RecipeErrorReporter.error("The " + getType() + " flag has invalid number: " + value);
            return false;
        }
        
        if(exp == 0)
        {
            RecipeErrorReporter.error("The " + getType() + " flag must not have 0 exp !");
            return false;
        }
        */
        
        return true;
    }
    
    @Override
    protected void onCheck(Args a)
    {
        if(!checkTime())
        {
            a.addReason(Messages.FLAG_REALTIME, message, "{mindate}", getMinDate(), "{maxdate}", getMaxDate());
        }
    }
}
