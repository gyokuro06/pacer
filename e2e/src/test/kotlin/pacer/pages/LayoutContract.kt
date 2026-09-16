package pacer.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole

object LayoutContract {
    const val MIN_TOUCH_TARGET_PX = 48.0
    const val MIN_MAIN_WIDTH_WIDE_PX = 512.0
    const val MIN_HEADING_FONT_PX = 64.0
    const val MIN_ROOM_CODE_FONT_PX = 32.0

    private fun px(value: Any?): Double = (value as Number).toDouble()

    fun assertViewportDeviceWidth(page: Page) {
        val content =
            page.locator("meta[name=\"viewport\"]")
                .getAttribute("content")
                ?.trim()
                .orEmpty()
        check(content.contains("width=device-width")) {
            "viewport meta に width=device-width がありません: \"$content\""
        }
    }

    fun assertNoHorizontalOverflow(page: Page) {
        val overflows =
            page.evaluate(
                """
                () => document.documentElement.scrollWidth > document.documentElement.clientWidth
                """.trimIndent(),
            ) as Boolean
        check(!overflows) {
            "横スクロールが発生しています " +
                "(scrollWidth=${page.evaluate("() => document.documentElement.scrollWidth")}, " +
                "clientWidth=${page.evaluate("() => document.documentElement.clientWidth")})"
        }
    }

    fun assertMinTouchTarget(page: Page, button: Locator, label: String) {
        val box = button.boundingBox()
        check(box != null && box.height >= MIN_TOUCH_TARGET_PX) {
            "$label のタッチターゲットが小さすぎます: height=${box?.height ?: "null"}px (min=${MIN_TOUCH_TARGET_PX}px)"
        }
    }

    fun assertHomePrimaryButtons(page: Page) {
        val main = page.getByRole(AriaRole.MAIN)
        // `/` auto-creates and lands on the room; primary actions live there.
        assertMinTouchTarget(
            page,
            main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("共有URLをコピー")),
            "共有URLをコピー",
        )
        assertMinTouchTarget(
            page,
            main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("スタート")),
            "スタート",
        )
    }

    fun assertRoomStartButton(page: Page) {
        val main = page.getByRole(AriaRole.MAIN)
        assertMinTouchTarget(
            page,
            main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("スタート")),
            "スタート",
        )
    }

    fun assertMainColumnWideEnough(page: Page) {
        val mainWidth =
            px(page.getByRole(AriaRole.MAIN).evaluate("el => el.getBoundingClientRect().width"))
        check(mainWidth >= MIN_MAIN_WIDTH_WIDE_PX) {
            "main カラムがワイド画面で十分な幅を使っていません: width=${mainWidth}px (min=${MIN_MAIN_WIDTH_WIDE_PX}px)"
        }
    }

    fun assertHomeBrandFluidTypography(page: Page) {
        val brand = page.getByRole(AriaRole.MAIN).getByRole(AriaRole.HEADING, Locator.GetByRoleOptions().setName("pacer"))
        val fontSize =
            px(
                brand.evaluate(
                    """
                    el => parseFloat(getComputedStyle(el).fontSize)
                    """.trimIndent(),
                ),
            )
        check(fontSize >= MIN_HEADING_FONT_PX) {
            "ホーム見出しの字体が小さすぎます: fontSize=${fontSize}px (min=${MIN_HEADING_FONT_PX}px)"
        }
    }

    fun assertRoomCodeFluidTypography(page: Page) {
        val roomCode =
            page.getByRole(AriaRole.MAIN)
                .getByRole(AriaRole.STATUS, Locator.GetByRoleOptions().setName("ルームコード"))
        val fontSize =
            px(
                roomCode.evaluate(
                    """
                    el => parseFloat(getComputedStyle(el).fontSize)
                    """.trimIndent(),
                ),
            )
        check(fontSize >= MIN_ROOM_CODE_FONT_PX) {
            "ルームコード表示の字体が小さすぎます: fontSize=${fontSize}px (min=${MIN_ROOM_CODE_FONT_PX}px)"
        }
    }
}
