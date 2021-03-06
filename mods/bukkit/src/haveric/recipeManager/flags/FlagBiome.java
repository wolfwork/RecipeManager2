package haveric.recipeManager.flags;

import haveric.recipeManager.ErrorReporter;
import haveric.recipeManager.Files;
import haveric.recipeManager.Messages;
import haveric.recipeManager.tools.Tools;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;


public class FlagBiome extends Flag {
    // Flag definition and documentation

    private static final FlagType TYPE = FlagType.BIOME;
    protected static final String[] A = new String[] {
        "{flag} <types> | [fail message]", };

    protected static final String[] D = new String[] {
        "Sets the biome required to allow crafting.",
        "Using this flag more than once will overwrite the previous one.",
        "",
        "For '<types>' you can list the biomes you want to allow or disallow.",
        "It needs at least one biome name and you can add more separated by , character.",
        "Also you can disallow biomes by prefixing them with ! character.",
        "Biomes: " + Tools.collectionToString(Arrays.asList(Biome.values())).toLowerCase(),
        "The biomes names can also be found in '" + Files.FILE_INFO_NAMES + "' file at 'BIOMES' section.", };

    protected static final String[] E = new String[] {
        "{flag} jungle, jungle_hills",
        "{flag} !mushroom_island, !mushroom_shore", };

    // Flag code

    private Map<Biome, Boolean> biomes = new EnumMap<Biome, Boolean>(Biome.class);
    private String failMessage;

    public FlagBiome() {
    }

    public FlagBiome(FlagBiome flag) {
        biomes.putAll(flag.biomes);
        failMessage = flag.failMessage;
    }

    @Override
    public FlagBiome clone() {
        super.clone();
        return new FlagBiome(this);
    }

    @Override
    public FlagType getType() {
        return TYPE;
    }

    public Map<Biome, Boolean> getBiomes() {
        return biomes;
    }

    public void setBiomes(EnumMap<Biome, Boolean> newBiomes) {
        Validate.notNull(newBiomes, "The 'biomes' argument must not be null!");

        biomes = newBiomes;
    }

    public void addBiome(Biome biome, boolean allowed) {
        biomes.put(biome, allowed);
    }

    public String getBiomesString(boolean allowed) {
        StringBuilder s = new StringBuilder();

        for (Entry<Biome, Boolean> e : biomes.entrySet()) {
            if (allowed == e.getValue().booleanValue()) {
                if (s.length() > 0) {
                    s.append(", ");
                }

                s.append(e.getKey());
            }
        }

        return s.toString();
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String newFailMessage) {
        failMessage = newFailMessage;
    }

    @Override
    protected boolean onParse(String value) {
        String[] split = value.split("\\|", 2);

        if (split.length > 1) {
            setFailMessage(split[1].trim());
        }

        split = split[0].trim().toLowerCase().split(",");

        for (String arg : split) {
            arg = arg.trim();
            boolean not = arg.charAt(0) == '!';

            if (not) {
                arg = arg.substring(1).trim();
            }

            Biome biome = Tools.parseEnum(arg, Biome.values());

            if (biome == null) {
                ErrorReporter.warning("Flag " + getType() + " has unknown biome: " + arg);
                continue;
            }

            addBiome(biome, !not);
        }

        return true;
    }

    @Override
    protected void onCheck(Args a) {
        if (!a.hasLocation()) {
            a.addCustomReason("Needs location!");
            return;
        }

        Block b = a.location().getBlock();
        Boolean set = getBiomes().get(b.getBiome());

        if (set == null) {
            a.addReason(Messages.FLAG_BIOME_ALLOWED, failMessage, "{biomes}", getBiomesString(true));
        } else if (!set) {
            a.addReason(Messages.FLAG_BIOME_UNALLOWED, failMessage, "{biomes}", getBiomesString(false));
        }
    }
}
