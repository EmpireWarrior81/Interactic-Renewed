package interactic;

import interactic.util.ServerSideConfigOption;
import empire.ewlib.config.Option;
import empire.ewlib.config.ui.ConfigScreen;
import empire.ewlib.config.ui.OptionComponentFactory;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public class InteracticConfigScreen extends ConfigScreen {

    protected InteracticConfigScreen(@Nullable Screen parent) {
        super(InteracticInit.getConfig(), parent);
    }

    @Override
    protected @Nullable OptionComponentFactory<?> factoryForOption(Option<?> option) {
        return InteracticInit.getConfig().clientOnlyMode() && option.backingField().hasAnnotation(ServerSideConfigOption.class)
                ? null
                : super.factoryForOption(option);
    }
}
