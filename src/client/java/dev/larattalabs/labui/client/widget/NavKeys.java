package dev.larattalabs.labui.client.widget;

import com.mojang.blaze3d.platform.InputConstants;
import dev.larattalabs.labui.ui.ListNav;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;

/** Maps key events to list navigation and activation. */
public final class NavKeys {
	private NavKeys() {
	}

	/** The navigation key, or null. */
	public static ListNav.@Nullable Key nav(KeyEvent e) {
		return switch (e.key()) {
			case InputConstants.KEY_UP -> ListNav.Key.UP;
			case InputConstants.KEY_DOWN -> ListNav.Key.DOWN;
			case InputConstants.KEY_LEFT -> ListNav.Key.LEFT;
			case InputConstants.KEY_RIGHT -> ListNav.Key.RIGHT;
			case InputConstants.KEY_HOME -> ListNav.Key.HOME;
			case InputConstants.KEY_END -> ListNav.Key.END;
			case InputConstants.KEY_PAGEUP -> ListNav.Key.PAGE_UP;
			case InputConstants.KEY_PAGEDOWN -> ListNav.Key.PAGE_DOWN;
			default -> null;
		};
	}

	/** Enter, keypad Enter or Space: what presses a focused control. */
	public static boolean activates(KeyEvent e) {
		int k = e.key();
		return k == InputConstants.KEY_RETURN || k == InputConstants.KEY_NUMPADENTER || k == InputConstants.KEY_SPACE;
	}

	public static boolean isEnter(KeyEvent e) {
		return e.key() == InputConstants.KEY_RETURN || e.key() == InputConstants.KEY_NUMPADENTER;
	}

	public static boolean isTab(KeyEvent e) {
		return e.key() == InputConstants.KEY_TAB;
	}
}
