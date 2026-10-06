package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import org.bukkit.entity.Player;

import java.util.List;

public class EcCommand extends AbstractCommand {

    public EcCommand(ServiceManager serviceManager) {
        super(serviceManager);
    }

    @Override
    protected String name() {
        return "ec";
    }

    @Override
    protected List<String> aliases() {
        return List.of(
                "enderchest",
                "echest"
        );
    }

    @Override
    protected void execute(Player player) {
        player.openInventory(player.getEnderChest());
    }
}