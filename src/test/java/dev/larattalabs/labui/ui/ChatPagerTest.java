package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class ChatPagerTest {
	private static List<Component> lines(int n) {
		List<Component> out = new ArrayList<>();
		for (int i = 1; i <= n; i++) {
			out.add(Component.literal("design-" + i + (i % 10 == 0 ? " oak" : "")));
		}
		return out;
	}

	private static List<ClickEvent> clicks(Component c) {
		List<ClickEvent> out = new ArrayList<>();
		c.visit((style, text) -> {
			if (style.getClickEvent() != null) {
				out.add(style.getClickEvent());
			}
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		return out;
	}

	@Test
	void pageWithFooterAndLinks() {
		List<Component> out = ChatPager.of(lines(41)).pageSize(8).page(2).pageCommand(n -> "/x list " + n).filterCommand("/x list 1 ").build();
		assertEquals(9, out.size());
		assertEquals("design-9", out.get(0).getString());
		Component footer = out.get(8);
		assertEquals("…and 25 more · page 2/6 [‹ prev] [next ›] [filter…]", footer.getString());
		List<ClickEvent> c = clicks(footer);
		assertEquals(3, c.size());
		assertEquals("/x list 1", ((ClickEvent.RunCommand) c.get(0)).command());
		assertEquals("/x list 3", ((ClickEvent.RunCommand) c.get(1)).command());
		assertInstanceOf(ClickEvent.SuggestCommand.class, c.get(2));
	}

	@Test
	void lastPageHasNoNextAndNoMore() {
		List<Component> out = ChatPager.of(lines(41)).pageSize(8).page(6).pageCommand(n -> "/x " + n).build();
		assertEquals(2, out.size());
		assertEquals("page 6/6 [‹ prev]", out.get(1).getString());
	}

	@Test
	void fitsOnOnePageWithoutFooter() {
		assertEquals(3, ChatPager.of(lines(3)).build().size());
	}

	@Test
	void filterKeepsMatchesOrSaysNothingMatches() {
		List<Component> out = ChatPager.of(lines(41)).pageSize(8).filter("oak").build();
		assertEquals(List.of("design-10 oak", "design-20 oak", "design-30 oak", "design-40 oak"), out.stream().map(Component::getString).toList());
		List<Component> none = ChatPager.of(lines(41)).filter("birch").build();
		assertEquals(1, none.size());
		assertEquals("Nothing matches “birch”.", none.get(0).getString());
	}
}
