package li.cil.oc.neoforge.client;

import com.google.common.base.Strings;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class Manual implements ManualAPI {
    public static final Manual INSTANCE = new Manual();

    public static final String LanguageKey = "%LANGUAGE%";
    public static final String FallbackLanguage = "en_US";
    public final List<Tab> tabs = new ArrayList<>();
    public final List<PathProvider> pathProviders = new ArrayList<>();
    public final List<ContentProvider> contentProviders = new ArrayList<>();
    public final List<Map.Entry<String, ImageProvider>> imageProviders = new ArrayList<>();
    public final Deque<History> history = new ArrayDeque<>();

    private Manual() {
        reset();
    }

    public static String makeRelative(String path, String base) {
        return li.cil.oc.neoforge.client.gui.Manual.resolveLink(path, base);
    }

    public void reset() {
        history.clear();
        history.push(new History(LanguageKey + "/index.md"));
    }

    @Override
    public void addTab(TabIconRenderer renderer, String tooltip, String path) {
        tabs.add(new Tab(renderer, tooltip, path));
        if (tabs.size() > 7) {
            OpenComputers.log().warn("Gosh I'm popular! Too many tabs were added to the OpenComputers in-game manual, so some won't be shown. In case this actually happens, let me know and I'll look into making them scrollable or something...");
        }
    }

    @Override
    public void addProvider(PathProvider provider) {
        pathProviders.add(provider);
    }

    @Override
    public void addProvider(ContentProvider provider) {
        contentProviders.add(provider);
    }

    @Override
    public void addProvider(String prefix, ImageProvider provider) {
        imageProviders.add(new AbstractMap.SimpleEntry<>(Strings.isNullOrEmpty(prefix) ? "" : prefix + ":", provider));
    }

    @Override
    public String pathFor(ItemStack stack) {
        for (PathProvider provider : pathProviders) {
            try {
                String path = provider.pathFor(stack);
                if (path != null) return path;
            } catch (Throwable t) {
                OpenComputers.log().warn("A path provider threw an error when queried with an item.", t);
            }
        }
        return null;
    }

    @Override
    public String pathFor(Level world, BlockPos pos) {
        for (PathProvider provider : pathProviders) {
            try {
                String path = provider.pathFor(world, pos);
                if (path != null) return path;
            } catch (Throwable t) {
                OpenComputers.log().warn("A path provider threw an error when queried with a block.", t);
            }
        }
        return null;
    }

    @Override
    public Iterable<String> contentFor(String path) {
        String cleanPath = com.google.common.io.Files.simplifyPath(path);
        String language;
        try {
            language = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Throwable t) {
            OpenComputers.log().warn("The game threw an error when querying current language.", t);
            language = FallbackLanguage;
        }
        Iterable<String> result = contentForWithRedirects(cleanPath.replaceAll(LanguageKey, language), new ArrayList<>());
        if (result != null) return result;
        result = contentForWithRedirects(cleanPath.replace(LanguageKey, FallbackLanguage), new ArrayList<>());
        return result;
    }

    @Override
    public ImageRenderer imageFor(String href) {
        for (int i = imageProviders.size() - 1; i >= 0; i--) {
            Map.Entry<String, ImageProvider> entry = imageProviders.get(i);
            String prefix = entry.getKey();
            if (href.startsWith(prefix)) {
                try {
                    ImageRenderer image = entry.getValue().getImage(href.substring(prefix.length()));
                    if (image != null) return image;
                } catch (Throwable t) {
                    OpenComputers.log().warn("An image provider threw an error when queried.", t);
                }
            }
        }
        return null;
    }

    @Override
    public void openFor(Player player) {
        if (player.level().isClientSide) {
            Minecraft.getInstance().setScreen(new li.cil.oc.neoforge.client.gui.Manual());
        }
    }

    @Override
    public void navigate(String path) {
        if (Minecraft.getInstance().screen instanceof li.cil.oc.neoforge.client.gui.Manual) {
            ((li.cil.oc.neoforge.client.gui.Manual) Minecraft.getInstance().screen).pushPage(path);
        } else {
            history.push(new History(path));
        }
    }

    private Iterable<String> contentForWithRedirects(String path, List<String> seen) {
        if (seen.contains(path)) {
            List<String> loop = new ArrayList<>();
            loop.add("Redirection loop: ");
            loop.addAll(seen);
            loop.add(path);
            return loop;
        }
        Iterable<String> content = doContentLookup(path);
        if (content != null) {
            Iterator<String> it = content.iterator();
            if (it.hasNext()) {
                String line = it.next();
                if (line.toLowerCase(Locale.ROOT).startsWith("#redirect ")) {
                    List<String> newSeen = new ArrayList<>(seen);
                    newSeen.add(path);
                    return contentForWithRedirects(makeRelative(line.substring("#redirect ".length()), path), newSeen);
                }
            }
            return content;
        }
        return null;
    }

    private Iterable<String> doContentLookup(String path) {
        for (ContentProvider provider : contentProviders) {
            try {
                Iterable<String> lines = provider.getContent(path);
                if (lines != null) return lines;
            } catch (Throwable t) {
                OpenComputers.log().warn("A content provider threw an error when queried.", t);
            }
        }
        return null;
    }

    public static class History {
        public final String path;
        public int offset;

        @SuppressWarnings("unused")
        public History(String path, int offset) {
            this.path = path;
            this.offset = offset;
        }

        @SuppressWarnings("unused")
        public History(String path) {
            this(path, 0);
        }
    }

    public record Tab(TabIconRenderer renderer, String tooltip, String path) {
    }
}
