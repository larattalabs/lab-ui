package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

/**
 * Pages a long chat list (common code, so servers can use it; unit-tested in {@code ChatPagerTest}). It shows
 * {@code pageSize} lines of page {@code page}, then one footer line: "…and K more · page 2/5 [‹ prev] [next ›]
 * [filter…]". The links run the commands the caller gives ({@code pageCommand.apply(n)}), so the pager knows nothing of
 * the mod's command tree. An optional filter keeps only the lines whose text matches ({@link TextMatch}); when nothing
 * matches, the one line says so.
 *
 * <pre>
 * for (Component c : ChatPager.of(lines).pageSize(8).page(p).filter(f)
 *         .pageCommand(n -&gt; "/architect list " + n + (f == null ? "" : " " + f))
 *         .filterCommand("/architect list 1 ").build()) {
 *     source.sendSystemMessage(c);
 * }
 * </pre>
 */
public final class ChatPager {
	public static final int DEFAULT_PAGE_SIZE = 8;
	private final List<Component> lines;
	private int pageSize = DEFAULT_PAGE_SIZE;
	private int page = 1;
	private @Nullable String filter;
	private @Nullable IntFunction<String> pageCommand;
	private @Nullable String filterCommand;

	private ChatPager(List<Component> lines) {
		this.lines = List.copyOf(lines);
	}

	public static ChatPager of(List<Component> lines) {
		return new ChatPager(lines);
	}

	public ChatPager pageSize(int n) {
		pageSize = Math.max(1, n);
		return this;
	}

	/** 1-based; clamped to the pages there are. */
	public ChatPager page(int p) {
		page = p;
		return this;
	}

	/** Keep only matching lines (null or blank = all). */
	public ChatPager filter(@Nullable String f) {
		filter = f == null || f.isBlank() ? null : f.strip();
		return this;
	}

	/** The command that shows page n (with the same filter); without it the footer has no [prev]/[next] links. */
	public ChatPager pageCommand(IntFunction<String> cmd) {
		pageCommand = cmd;
		return this;
	}

	/** A command prefix put in the chat box by [filter…] (a suggest, not a run, so the player types the filter). */
	public ChatPager filterCommand(String prefix) {
		filterCommand = prefix;
		return this;
	}

	/** The lines that pass the filter. */
	public List<Component> matching() {
		if (filter == null) {
			return lines;
		}
		List<Component> out = new ArrayList<>();
		for (Component c : lines) {
			if (TextMatch.matches(filter, c.getString(), List.of())) {
				out.add(c);
			}
		}
		return out;
	}

	public PageSlice slice() {
		return PageSlice.of(matching().size(), pageSize, page);
	}

	/** The page's lines plus the footer (no footer when everything fits on one page). */
	public List<Component> build() {
		List<Component> m = matching();
		List<Component> out = new ArrayList<>();
		if (m.isEmpty()) {
			out.add(Component.literal(filter != null ? "Nothing matches “" + filter + "”." : "Nothing to show.")
				.withStyle(ChatFormatting.GRAY));
			return out;
		}
		PageSlice s = PageSlice.of(m.size(), pageSize, page);
		out.addAll(m.subList(s.from(), s.to()));
		if (s.pages() > 1) {
			out.add(footer(s));
		}
		return out;
	}

	/** The footer line for a slice. */
	public Component footer(PageSlice s) {
		MutableComponent f = Component.empty();
		if (s.after() > 0) {
			f.append(Component.literal("…and " + s.after() + " more · ").withStyle(ChatFormatting.GRAY));
		}
		f.append(Component.literal("page " + s.page() + "/" + s.pages()).withStyle(ChatFormatting.GRAY));
		if (pageCommand != null) {
			if (s.hasPrev()) {
				f.append(" ").append(link("[‹ prev]", new ClickEvent.RunCommand(pageCommand.apply(s.page() - 1)), "Show page " + (s.page() - 1)));
			}
			if (s.hasNext()) {
				f.append(" ").append(link("[next ›]", new ClickEvent.RunCommand(pageCommand.apply(s.page() + 1)), "Show page " + (s.page() + 1)));
			}
		}
		if (filterCommand != null) {
			f.append(" ").append(link("[filter…]", new ClickEvent.SuggestCommand(filterCommand), "Type a filter"));
		}
		return f;
	}

	private static Component link(String text, ClickEvent click, String hover) {
		return Component.literal(text).withStyle(st -> st.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(click)
			.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}
}
