package haveric.recipeManager.commands;

import haveric.recipeManager.Messages;
import haveric.recipeManager.RecipeManager;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;


public class HelpCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PluginDescriptionFile desc = RecipeManager.getPlugin().getDescription();

        Messages.send(sender, ChatColor.YELLOW + "------ " + ChatColor.WHITE + desc.getFullName() + ChatColor.GRAY + " by haveric / Digi " + ChatColor.YELLOW + "------");

        Map<String, Map<String, Object>> cmds = desc.getCommands();
        Map<String, Object> data;

        for (Entry<String, Map<String, Object>> e : cmds.entrySet()) {
            data = e.getValue();
            Object obj = data.get("permission");

            if (obj instanceof String && !sender.hasPermission((String) obj)) {
                continue;
            }

            Messages.send(sender, "<gold>" + data.get("usage").toString().replace("<command>", e.getKey()) + ": " + ChatColor.RESET + data.get("description"));
        }

        return true;
    }
}
