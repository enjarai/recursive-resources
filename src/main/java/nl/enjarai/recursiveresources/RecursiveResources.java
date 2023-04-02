package nl.enjarai.recursiveresources;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;
import nl.enjarai.cicada.api.conversation.ConversationManager;
import nl.enjarai.cicada.api.util.CicadaEntrypoint;
import nl.enjarai.cicada.api.util.JsonSource;
import nl.enjarai.cicada.api.util.ProperLogger;
import org.slf4j.Logger;

public class RecursiveResources implements ClientModInitializer, CicadaEntrypoint {
    public static final String MOD_ID = "recursiveresources";
    public static final Logger LOGGER = ProperLogger.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {

    }

    @Override
    public void registerConversations(ConversationManager conversationManager) {
        conversationManager.registerSource(
                JsonSource.fromUrl("https://raw.githubusercontent.com/enjarai/recursive-resources/master/src/main/resources/cicada/recursiveresources/conversations.json")
                        .or(JsonSource.fromResource("cicada/recursiveresources/conversations.json")),
                LOGGER::info
        );
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
