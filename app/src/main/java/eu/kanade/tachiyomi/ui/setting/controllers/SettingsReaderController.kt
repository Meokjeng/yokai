package eu.kanade.tachiyomi.ui.setting.controllers

import android.content.ComponentName
import android.content.Intent
import androidx.preference.PreferenceScreen
import dev.yokai.domain.ui.settings.ReaderPreferences
import dev.yokai.domain.ui.settings.ReaderPreferences.CutoutBehaviour
import dev.yokai.domain.ui.settings.ReaderPreferences.LandscapeCutoutBehaviour
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues
import eu.kanade.tachiyomi.data.preference.changesIn
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBackgroundColor
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.bindTo
import eu.kanade.tachiyomi.ui.setting.defaultValue
import eu.kanade.tachiyomi.ui.setting.infoPreference
import eu.kanade.tachiyomi.ui.setting.intListPreference
import eu.kanade.tachiyomi.ui.setting.listPreference
import eu.kanade.tachiyomi.ui.setting.multiSelectListPreferenceMat
import eu.kanade.tachiyomi.ui.setting.onClick
import eu.kanade.tachiyomi.ui.setting.preference
import eu.kanade.tachiyomi.ui.setting.preferenceCategory
import eu.kanade.tachiyomi.ui.setting.summaryRes
import eu.kanade.tachiyomi.ui.setting.switchPreference
import eu.kanade.tachiyomi.ui.setting.titleRes
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isTablet
import eu.kanade.tachiyomi.util.view.activityBinding
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsReaderController : SettingsLegacyController() {

    private val readerPreferences: ReaderPreferences by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.reader

        preferenceCategory {
            titleRes = R.string.general
            intListPreference(activity) {
                key = Keys.defaultReadingMode
                titleRes = R.string.default_reading_mode
                entriesRes = ReadingModeType.entries.drop(1)
                    .map { value -> value.stringRes }.toTypedArray()
                entryValues = ReadingModeType.entries.drop(1)
                    .map { value -> value.flagValue }
                defaultValue = 2
            }
            intListPreference(activity) {
                key = Keys.doubleTapAnimationSpeed
                titleRes = R.string.double_tap_anim_speed
                entries = listOf(
                    context.getString(R.string.no_animation),
                    context.getString(
                        R.string.fast,
                    ),
                    context.getString(R.string.normal),
                )
                entryValues = listOf(1, 250, 500) // using a value of 0 breaks the image viewer, so
                // min is 1
                defaultValue = 500
            }
            switchPreference {
                key = Keys.enableTransitions
                titleRes = R.string.animate_page_transitions
                defaultValue = true
            }
            intListPreference(activity) {
                key = Keys.preloadSize
                titleRes = R.string.page_preload_amount
                entryValues = listOf(4, 6, 8, 10, 12, 14, 16, 20)
                entries = entryValues.map { context.resources.getQuantityString(R.plurals.pages_plural, it, it) }
                defaultValue = 6
                summaryRes = R.string.amount_of_pages_to_preload
            }
            multiSelectListPreferenceMat(activity) {
                key = Keys.readerBottomButtons
                titleRes = R.string.display_buttons_bottom_reader
                val enumConstants = ReaderBottomButton.entries
                entriesRes = ReaderBottomButton.entries.map { it.stringRes }.toTypedArray()
                entryValues = enumConstants.map { it.value }
                allSelectionRes = R.string.display_options
                allIsAlwaysSelected = true
                showAllLast = true
                val defaults = ReaderBottomButton.BUTTONS_DEFAULTS.toMutableList()
                if (context.isTablet()) {
                    defaults.add(ReaderBottomButton.ShiftDoublePage.value)
                }
                defaultValue = defaults
            }
            infoPreference(R.string.certain_buttons_can_be_found)
        }

        preferenceCategory {
            titleRes = R.string.display

            intListPreference(activity) {
                key = Keys.defaultOrientationType
                titleRes = R.string.default_orientation
                val enumConstants = OrientationType.entries.drop(1)
                entriesRes = enumConstants.map { it.stringRes }.toTypedArray()
                entryValues = enumConstants.map { value -> value.flagValue }
                defaultValue = OrientationType.FREE.flagValue
            }
            intListPreference(activity) {
                key = Keys.readerTheme
                titleRes = R.string.background_color
                val enumConstants = ReaderBackgroundColor.entries
                entriesRes = enumConstants.map { it.longStringRes ?: it.stringRes }.toTypedArray()
                entryValues = enumConstants.map { it.prefValue }
                defaultValue = ReaderBackgroundColor.SMART_PAGE.prefValue
            }
            switchPreference {
                key = Keys.fullscreen
                titleRes = R.string.fullscreen
                defaultValue = true
            }
            switchPreference {
                bindTo(readerPreferences.cutoutShort())
                // FIXME: Transition from reader to homepage is broken when cutout short is disabled
                title = context.getString(R.string.pref_cutout_short).addBetaTag(context)

                preferences.fullscreen().changesIn(viewScope) {
                    isVisible = DeviceUtil.hasCutout(activity).ordinal >= DeviceUtil.CutoutSupport.MODERN.ordinal && it
                }
            }
            if (DeviceUtil.isVivo && DeviceUtil.hasCutout(activity) == DeviceUtil.CutoutSupport.LEGACY) {
                preference {
                    title = context.getString(R.string.pref_legacy_cutout).addBetaTag(context)
                    summaryRes = R.string.pref_legacy_cutout_info

                    onClick {
                        val intent = Intent().apply {
                            setComponent(ComponentName("com.android.settings", "com.vivo.settings.display.FullScreenDisplayActivity"))
                        }
                        startActivity(intent)
                    }
                }
            }
            listPreference(activity) {
                bindTo(readerPreferences.landscapeCutoutBehavior())
                title = "${context.getString(R.string.cutout_area_behavior)} (${context.getString(R.string.landscape)})"
                val values = LandscapeCutoutBehaviour.entries
                entriesRes = values.map { it.titleResId }.toTypedArray()
                entryValues = values.map { it.name }

                preferences.fullscreen().changesIn(viewScope) {
                    isVisible = DeviceUtil.hasCutout(activity).ordinal >= DeviceUtil.CutoutSupport.MODERN.ordinal && it
                }
            }
            switchPreference {
                key = Keys.keepScreenOn
                titleRes = R.string.keep_screen_on
                defaultValue = true
            }
            switchPreference {
                key = Keys.showPageNumber
                titleRes = R.string.show_page_number
                defaultValue = true
            }
        }

        preferenceCategory {
            titleRes = R.string.reading

            switchPreference {
                key = Keys.skipRead
                titleRes = R.string.skip_read_chapters
                defaultValue = false
            }
            switchPreference {
                key = Keys.skipFiltered
                titleRes = R.string.skip_filtered_chapters
                defaultValue = true
            }
            switchPreference {
                bindTo(preferences.skipDupe())
                titleRes = R.string.skip_dupe_chapters
            }
            switchPreference {
                key = Keys.alwaysShowChapterTransition
                titleRes = R.string.always_show_chapter_transition
                summaryRes = R.string.if_disabled_transition_will_skip
                defaultValue = true
            }
        }

        preferenceCategory {
            titleRes = R.string.paged

            intListPreference(activity) {
                key = Keys.navigationModePager
                titleRes = R.string.tap_zones
                entries = context.resources.getStringArray(R.array.reader_nav).also { values ->
                    entryRange = 0..values.size
                }.toList()
                defaultValue = "0"
            }
            listPreference(activity) {
                key = Keys.pagerNavInverted
                titleRes = R.string.invert_tapping
                entriesRes = arrayOf(
                    R.string.none,
                    R.string.horizontally,
                    R.string.vertically,
                    R.string.both_axes,
                )
                entryValues = listOf(
                    ViewerNavigation.TappingInvertMode.NONE.name,
                    ViewerNavigation.TappingInvertMode.HORIZONTAL.name,
                    ViewerNavigation.TappingInvertMode.VERTICAL.name,
                    ViewerNavigation.TappingInvertMode.BOTH.name,
                )
                defaultValue = ViewerNavigation.TappingInvertMode.NONE.name
            }

            intListPreference(activity) {
                key = Keys.imageScaleType
                titleRes = R.string.scale_type
                entriesRes = arrayOf(
                    R.string.fit_screen,
                    R.string.stretch,
                    R.string.fit_width,
                    R.string.fit_height,
                    R.string.original_size,
                    R.string.smart_fit,
                )
                entryRange = 1..6
                defaultValue = 1
            }

            listPreference(activity) {
                bindTo(readerPreferences.pagerCutoutBehavior())
                titleRes = R.string.cutout_area_behavior
                val values = CutoutBehaviour.entries
                entriesRes = values.map { it.titleResId }.toTypedArray()
                entryValues = values.map { it.name }

                // Calling this once to show only on cutout
                isVisible = DeviceUtil.hasCutout(activity).ordinal >= DeviceUtil.CutoutSupport.LEGACY.ordinal

                // Calling this a second time in case activity is recreated while on this page
                // Keep the first so it shouldn't animate hiding the preference for phones without
                // cutouts
                activityBinding?.root?.post {
                    isVisible = DeviceUtil.hasCutout(activity).ordinal >= DeviceUtil.CutoutSupport.LEGACY.ordinal
                }
            }

            switchPreference {
                bindTo(preferences.landscapeZoom())
                titleRes = R.string.zoom_double_page_spreads
                visibleIf(preferences.imageScaleType()) { it == 1 }
            }
            intListPreference(activity) {
                key = Keys.zoomStart
                titleRes = R.string.zoom_start_position
                entriesRes = arrayOf(
                    R.string.automatic,
                    R.string.left,
                    R.string.right,
                    R.string.center,
                )
                entryRange = 1..4
                defaultValue = 1
            }
            switchPreference {
                key = Keys.cropBorders
                titleRes = R.string.crop_borders
                defaultValue = false
            }
            switchPreference {
                bindTo(preferences.navigateToPan())
                titleRes = R.string.navigate_pan
            }
            intListPreference(activity) {
                key = Keys.pageLayout
                title = context.getString(R.string.page_layout).addBetaTag(context)
                dialogTitleRes = R.string.page_layout
                val enumConstants = PageLayout.entries
                entriesRes = enumConstants.map { it.fullStringRes }.toTypedArray()
                entryValues = enumConstants.map { it.value }
                defaultValue = PageLayout.AUTOMATIC.value
            }
            infoPreference(R.string.automatic_can_still_switch).apply {
                preferences.pageLayout().changesIn(viewScope) { isVisible = it == PageLayout.AUTOMATIC.value }
            }
            switchPreference {
                key = Keys.automaticSplitsPage
                titleRes = R.string.split_double_pages_portrait
                defaultValue = false
                preferences.pageLayout().changesIn(viewScope) { isVisible = it == PageLayout.AUTOMATIC.value }
            }
            switchPreference {
                key = Keys.invertDoublePages
                titleRes = R.string.invert_double_pages
                defaultValue = false
                preferences.pageLayout().changesIn(viewScope) { isVisible = it != PageLayout.SINGLE_PAGE.value }
            }
        }
        preferenceCategory {
            titleRes = R.string.long_strip

            intListPreference(activity) {
                key = Keys.navigationModeWebtoon
                titleRes = R.string.tap_zones
                entries = context.resources.getStringArray(R.array.reader_nav).also { values ->
                    entryRange = 0..values.size
                }.toList()
                defaultValue = "0"
            }
            listPreference(activity) {
                key = Keys.webtoonNavInverted
                titleRes = R.string.invert_tapping
                entriesRes = arrayOf(
                    R.string.none,
                    R.string.horizontally,
                    R.string.vertically,
                    R.string.both_axes,
                )
                entryValues = listOf(
                    ViewerNavigation.TappingInvertMode.NONE.name,
                    ViewerNavigation.TappingInvertMode.HORIZONTAL.name,
                    ViewerNavigation.TappingInvertMode.VERTICAL.name,
                    ViewerNavigation.TappingInvertMode.BOTH.name,
                )
                defaultValue = ViewerNavigation.TappingInvertMode.NONE.name
            }
            listPreference(activity) {
                bindTo(preferences.webtoonReaderHideThreshold())
                titleRes = R.string.pref_hide_threshold
                val enumValues = PreferenceValues.ReaderHideThreshold.entries
                entriesRes = enumValues.map { it.titleResId }.toTypedArray()
                entryValues = enumValues.map { it.name }
            }
            switchPreference {
                key = Keys.cropBordersWebtoon
                titleRes = R.string.crop_borders
                defaultValue = false
            }

            intListPreference(activity) {
                key = Keys.webtoonSidePadding
                titleRes = R.string.pref_long_strip_side_padding
                entriesRes = arrayOf(
                    R.string.long_strip_side_padding_0,
                    R.string.long_strip_side_padding_5,
                    R.string.long_strip_side_padding_10,
                    R.string.long_strip_side_padding_15,
                    R.string.long_strip_side_padding_20,
                    R.string.long_strip_side_padding_25,
                )
                entryValues = listOf(0, 5, 10, 15, 20, 25)
                defaultValue = "0"
            }

            intListPreference(activity) {
                key = Keys.webtoonPageLayout
                title = context.getString(R.string.page_layout)
                dialogTitleRes = R.string.page_layout
                val enumConstants = arrayOf(PageLayout.SINGLE_PAGE, PageLayout.SPLIT_PAGES)
                entriesRes = enumConstants.map { it.fullStringRes }.toTypedArray()
                entryValues = enumConstants.map { it.webtoonValue }
                defaultValue = PageLayout.SINGLE_PAGE.value
            }

            switchPreference {
                key = Keys.webtoonInvertDoublePages
                titleRes = R.string.invert_double_pages
                defaultValue = false
            }

            switchPreference {
                key = Keys.webtoonEnableZoomOut
                titleRes = R.string.enable_zoom_out
                defaultValue = false
            }
        }
        preferenceCategory {
            titleRes = R.string.navigation

            switchPreference {
                key = Keys.readWithVolumeKeys
                titleRes = R.string.volume_keys
                defaultValue = false
            }
            switchPreference {
                key = Keys.readWithVolumeKeysInverted
                titleRes = R.string.invert_volume_keys
                defaultValue = false

                preferences.readWithVolumeKeys().changesIn(viewScope) { isVisible = it }
            }
        }

        preferenceCategory {
            titleRes = R.string.actions

            switchPreference {
                key = Keys.readWithLongTap
                titleRes = R.string.show_on_long_press
                defaultValue = true
            }
            switchPreference {
                bindTo(preferences.folderPerManga())
                titleRes = R.string.save_pages_separately
                summaryRes = R.string.create_folders_by_manga_title
            }
        }
    }
}