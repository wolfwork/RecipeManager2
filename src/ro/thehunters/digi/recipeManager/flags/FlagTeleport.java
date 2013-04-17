package ro.thehunters.digi.recipeManager.flags;

public class FlagTeleport extends Flag
{
    // Flag definition and documentation
    
    private static final FlagType TYPE;
    protected static final String[] A;
    protected static final String[] D;
    protected static final String[] E;
    
    static
    {
        TYPE = FlagType.TELEPORT;
        
        A = new String[]
        {
            "{flag} ...",
        };
        
        D = new String[]
        {
            "FLAG NOT YET IMPLEMENTED !",
        };
        
        E = null;
    }
    
    // Flag code
    
    public FlagTeleport()
    {
    }
    
    public FlagTeleport(FlagTeleport flag)
    {
        // TODO clone
    }
    
    @Override
    public FlagTeleport clone()
    {
        return new FlagTeleport(this);
    }
    
    @Override
    public FlagType getType()
    {
        return TYPE;
    }
    
    @Override
    protected boolean onParse(String value)
    {
        // TODO
        
        // @teleport relative block | y + 2 | x - 2
        // @teleport relative player | y + 10
        
        return true;
    }
    
    @Override
    protected void onCrafted(Args a)
    {
    }
}
