package haveric.recipeManager.commands;

import haveric.recipeManager.Messages;
import haveric.recipeManager.RecipeManager;
import haveric.recipeManager.data.RecipeBook;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;


public class GetBookCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command can only be used by players.");
            return true;
        }

        int argsLength = args.length;
        if (argsLength == 0) {
            return false;
        }

        Player player = (Player) sender;

        StringBuilder s = new StringBuilder(argsLength * 10);

        s.append(args[0]);

        for (int i = 1; i < argsLength; i++) {
            s.append(' ').append(args[i]);
        }

        String bookName = s.toString();
        int volume = 1;
        int index = bookName.lastIndexOf('#');

        if (index > 0) { // found and not the first character
            try {
                volume = Integer.parseInt(bookName.substring(index + 1));
            } catch (Throwable e) {
                Messages.CMD_GETBOOK_INVALIDNUMBER.print(sender);
            }

            bookName = bookName.substring(0, index).trim();
        }

        List<RecipeBook> books = RecipeManager.getRecipeBooks().getBooksPartialMatch(bookName);

        if (books.isEmpty()) {
            Messages.CMD_GETBOOK_NOTEXIST.print(sender, null, "{arg}", bookName);
            return true;
        } else if (books.size() > 1) {
            Messages.CMD_GETBOOK_MANYMATCHES.print(sender, null, "{num}", books.size(), "{arg}", bookName);

            for (RecipeBook b : books) {
                Messages.send(sender, "<red> - <reset>" + b.getTitle());
            }

            return true;
        }

        ItemStack item = books.get(0).getBookItem(volume);

        player.getInventory().addItem(item);

        BookMeta meta = (BookMeta) item.getItemMeta();

        Messages.CMD_GETBOOK_GIVEN.print(sender, null, "{title}", meta.getTitle());

        return true;
    }
}
