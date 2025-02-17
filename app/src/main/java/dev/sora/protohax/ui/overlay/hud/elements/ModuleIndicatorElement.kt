package dev.sora.protohax.ui.overlay.hud.elements

import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.protohax.ui.overlay.hud.HudAlignment
import dev.sora.protohax.ui.overlay.hud.HudElement
import dev.sora.protohax.ui.overlay.hud.HudManager
import dev.sora.protohax.ui.overlay.menu.tabs.i18nNormalization
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.EventModuleToggle
import dev.sora.relay.cheat.value.NamedChoice
import java.util.concurrent.atomic.AtomicBoolean

class ModuleIndicatorElement : HudElement(HudManager.MODULE_INDICATOR_ELEMENT_IDENTIFIER) {

	private var sortingModeValue by listValue(
		"SortingMode",
		SortingMode.values(),
		SortingMode.LENGTH_DESCENDING
	)
	private var textRTLValue by boolValue("TextRTL", true)
	private var colorModeValue by listValue("ColorMode", ColorMode.values(), ColorMode.HUE)
	private var colorReversedSortValue by boolValue("ColorReversedSort", false)
	private var colorRedValue by intValue(
		"ColorRed",
		255,
		0..255
	).visible { colorModeValue != ColorMode.HUE }
	private var colorGreenValue by intValue(
		"ColorGreen",
		255,
		0..255
	).visible { colorModeValue != ColorMode.HUE }
	private var colorBlueValue by intValue(
		"ColorBlue",
		255,
		0..255
	).visible { colorModeValue != ColorMode.HUE }
	private var textSizeValue by intValue("TextSize", 15, 10..50).listen {
		paint.textSize = it * MyApplication.density
		it
	}
	private var spacingValue by intValue("Spacing", 3, 0..20)

	private val paint = TextPaint().also {
		it.color = Color.WHITE
		it.isAntiAlias = true
		it.textSize = 20 * MyApplication.density
	}

	override var height = 10f
		private set
	override var width = 10f
		private set

	init {
		alignmentValue = HudAlignment.RIGHT_TOP
		posX = 0
		posY = 0
	}

	override fun onRender(canvas: Canvas, editMode: Boolean, needRefresh: AtomicBoolean) {
		val modules = sortingModeValue.getModules(paint)
		val lineHeight = paint.fontMetrics.let { it.descent - it.ascent }
		if (modules.isEmpty()) {
			if (editMode) {
				if (height != lineHeight) {
					needRefresh.set(true)
				}
				height = lineHeight
				val alertNoModules = "No modules has toggled on currently"
				width = paint.measureText(alertNoModules)

				paint.color =
					colorModeValue.getColor(0, 1, colorRedValue, colorGreenValue, colorBlueValue)
				canvas.drawText(alertNoModules, 0f, -paint.fontMetrics.ascent, paint)
			}
			return
		}

		var y = 0f
		val lineSpacing = (spacingValue * MyApplication.density)
		val maxWidth = modules.maxOf { paint.measureText(i18nNormalization(it.displayName)) }
		modules.forEachIndexed { i, module ->
			paint.color = colorModeValue.getColor(
				if (colorReversedSortValue) modules.size - i else i,
				modules.size,
				colorRedValue,
				colorGreenValue,
				colorBlueValue
			)
			canvas.drawText(
				i18nNormalization(module.displayName),
				if (textRTLValue) maxWidth - paint.measureText(i18nNormalization(module.displayName)) else 0f,
				-paint.fontMetrics.ascent + y,
				paint
			)
			y += lineHeight + lineSpacing
		}
		y -= lineHeight
		if (height != y) {
			needRefresh.set(true)
			height = y
		}
		if (width != maxWidth) {
			needRefresh.set(true)
			width = maxWidth
		}
	}

	private val onModuleToggle = handle<EventModuleToggle> {
		session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
	}

	enum class SortingMode(override val choiceName: String) : NamedChoice {
		NAME_ASCENDING("NameAscending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { i18nNormalization(it.displayName) }
			}
		},
		NAME_DESCENDING("NameDescending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { i18nNormalization(it.displayName) }
					.reversed()
			}
		},
		LENGTH_ASCENDING("LengthAscending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { paint.measureText(i18nNormalization(it.displayName)) }
			}
		},
		LENGTH_DESCENDING("LengthDescending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { paint.measureText(i18nNormalization(it.displayName)) }
					.reversed()
			}
		};

		abstract fun getModules(paint: TextPaint): List<CheatModule>
	}

	enum class ColorMode(override val choiceName: String) : NamedChoice {
		CUSTOM("Custom") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				return Color.rgb(r, g, b)
			}
		},
		HUE("Hue") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				return dev.sora.protohax.util.Color.HSBtoRGB(
					index.toFloat() / ( size + (System.currentTimeMillis() % 20)),
					1f,
					1f
				)
			}
		},
		SATURATION_SHIFT_ASCENDING("SaturationShift") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				val hsv = floatArrayOf(0f, 0f, 0f)
				Color.colorToHSV(Color.rgb(r, g, b), hsv)
				hsv[2] = index.toFloat() / size
				return Color.HSVToColor(hsv)
			}
		};

		abstract fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int
	}
}
