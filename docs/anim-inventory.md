# Inventario de constantes de animación (Launcher3 android17-release)

Generado por `tools/anim-inventory.py`. «Redirigible» = se puede cambiar sin tocar código (recurso entero superpuesto por `tools/apply-anim-profile.py`).

## A. Recursos enteros (`res/values/config.xml`) — redirigibles

| Recurso | Valor AOSP | Evento del perfil |
|---|---|---|
| `config_pageSnapAnimationDuration` | 750 ms | home.pageSnap |
| `config_keyboardTaskFocusSnapAnimationDuration` | 750 ms | (teclado, tareas) |
| `config_dropAnimMinDuration` | 100 ms | icon.drop (mín) |
| `config_dropAnimMaxDuration` | 500 ms | icon.drop (máx) |
| `config_materialFolderExpandDuration` | 200 ms | folder.open / folder.close |
| `config_folderDelay` | 30 ms | (retardo carpeta) |
| `config_caretAnimationDuration` | 200 ms | (caret del cajón) |
| `config_bottomSheetOpenDuration` | 267 ms | (hoja inferior: widgets/popups) |
| `config_bottomSheetCloseDuration` | 267 ms | (hoja inferior) |
| `config_allAppsOpenDuration` | 600 ms | drawer.open |
| `config_allAppsCloseDuration` | 300 ms | drawer.close |

## B. Constantes en código — NO redirigidas (requieren parche en AOSP)

| Archivo | Constante | Valor |
|---|---|---|
| `com/android/launcher3/AppWidgetResizeFrame.kt` | `SNAP_DURATION_MS` | 150 |
| `com/android/launcher3/BaseActivity.java` | `PENDING_INVISIBLE_BY_WALLPAPER_ANIMATION` | 1 |
| `com/android/launcher3/ButtonDropTarget.java` | `DRAG_VIEW_DROP_DURATION` | 285 |
| `com/android/launcher3/CellLayout.java` | `REORDER_ANIMATION_DURATION` | 150 |
| `com/android/launcher3/DropTargetBar.java` | `DEFAULT_DRAG_FADE_DURATION` | 175 |
| `com/android/launcher3/Hotseat.java` | `BUBBLE_BAR_ADJUSTMENT_ANIMATION_DURATION_MS` | 250 |
| `com/android/launcher3/Launcher.java` | `BOUNCE_ANIMATION_TENSION` | 1 |
| `com/android/launcher3/Launcher.java` | `ON_ACTIVITY_RESULT_ANIMATION_DELAY` | 500 |
| `com/android/launcher3/Launcher.java` | `NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS` | 5 |
| `com/android/launcher3/Launcher.java` | `NEW_APPS_ANIMATION_DELAY` | 500 |
| `com/android/launcher3/LauncherAnimUtils.java` | `SCALE_INDEX_UNFOLD_ANIMATION` | 1 |
| `com/android/launcher3/LauncherAnimUtils.java` | `SCALE_INDEX_REVEAL_ANIM` | 3 |
| `com/android/launcher3/LauncherAnimUtils.java` | `SCALE_INDEX_FOLDER_ANIM` | 5 |
| `com/android/launcher3/Workspace.java` | `ADJACENT_SCREEN_DROP_DURATION` | 300 |
| `com/android/launcher3/Workspace.java` | `ANIMATE_INTO_POSITION_AND_DISAPPEAR` | 0 |
| `com/android/launcher3/Workspace.java` | `ANIMATE_INTO_POSITION_AND_REMAIN` | 1 |
| `com/android/launcher3/Workspace.java` | `ANIMATE_INTO_POSITION_AND_RESIZE` | 2 |
| `com/android/launcher3/Workspace.java` | `COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION` | 3 |
| `com/android/launcher3/Workspace.java` | `CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION` | 4 |
| `com/android/launcher3/allapps/ActivityAllAppsContainerView.java` | `DEFAULT_SEARCH_TRANSITION_DURATION_MS` | 300 |
| `com/android/launcher3/allapps/AllAppsTransitionController.java` | `REVERT_SWIPE_ALL_APPS_TO_HOME_ANIMATION_DURATION_MS` | 200 |
| `com/android/launcher3/allapps/PrivateProfileManager.java` | `EXPAND_COLLAPSE_DURATION` | 400 |
| `com/android/launcher3/allapps/PrivateProfileManager.java` | `SETTINGS_OPACITY_DURATION` | 400 |
| `com/android/launcher3/allapps/PrivateProfileManager.java` | `TEXT_UNLOCK_OPACITY_DURATION` | 300 |
| `com/android/launcher3/allapps/PrivateProfileManager.java` | `TEXT_LOCK_OPACITY_DURATION` | 50 |
| `com/android/launcher3/allapps/PrivateProfileManager.java` | `APP_OPACITY_DURATION` | 400 |
| `com/android/launcher3/allapps/PrivateProfileManager.java` | `MASK_VIEW_DURATION` | 200 |
| `com/android/launcher3/allapps/PrivateProfileManager.java` | `CONTAINER_OPACITY_DURATION` | 150 |
| `com/android/launcher3/allapps/RecyclerViewAnimationController.java` | `CONTENT_FADE_PROGRESS_DURATION` | 0 |
| `com/android/launcher3/allapps/RecyclerViewAnimationController.java` | `BACKGROUND_FADE_PROGRESS_DURATION` | 0 |
| `com/android/launcher3/allapps/WorkUtilityView.java` | `TEXT_EXPAND_OPACITY_DURATION` | 300 |
| `com/android/launcher3/allapps/WorkUtilityView.java` | `TEXT_COLLAPSE_OPACITY_DURATION` | 50 |
| `com/android/launcher3/allapps/WorkUtilityView.java` | `EXPAND_COLLAPSE_DURATION` | 300 |
| `com/android/launcher3/anim/AnimatorPlaybackController.java` | `ANIMATION_COMPLETE_THRESHOLD` | 0 |
| `com/android/launcher3/apppairs/AppPairIcon.kt` | `HOVER_SCALE_DURATION` | 150 |
| `com/android/launcher3/celllayout/ReorderPreviewAnimation.kt` | `PREVIEW_DURATION` | 300 |
| `com/android/launcher3/dragndrop/AddItemActivity.kt` | `ACTIVITY_SLIDE_IN_DURATION_MS` | 150L |
| `com/android/launcher3/dragndrop/DragLayer.java` | `ANIMATION_END_DISAPPEAR` | 0 |
| `com/android/launcher3/dragndrop/DragLayer.java` | `ANIMATION_END_REMAIN_VISIBLE` | 2 |
| `com/android/launcher3/dragndrop/DragView.java` | `VIEW_ZOOM_DURATION` | 150 |
| `com/android/launcher3/folder/Folder.java` | `STATE_ANIMATING` | 1 |
| `com/android/launcher3/folder/Folder.java` | `SCROLL_HINT_DURATION` | 500 |
| `com/android/launcher3/folder/Folder.java` | `FOLDER_NAME_ANIMATION_DURATION` | 633 |
| `com/android/launcher3/folder/Folder.java` | `FOLDER_COLOR_ANIMATION_DURATION` | 200 |
| `com/android/launcher3/folder/FolderAnimationManager.java` | `FOLDER_NAME_ALPHA_DURATION` | 32 |
| `com/android/launcher3/folder/FolderAnimationManager.java` | `LARGE_FOLDER_FOOTER_DURATION` | 128 |
| `com/android/launcher3/folder/FolderIcon.java` | `DROP_IN_ANIMATION_DURATION` | 400 |
| `com/android/launcher3/folder/FolderPagedView.java` | `REORDER_ANIMATION_DURATION` | 230 |
| `com/android/launcher3/folder/FolderSpringAnimatorSet.kt` | `FOLDER_NAME_ALPHA_DURATION` | 32 |
| `com/android/launcher3/folder/FolderSpringAnimatorSet.kt` | `LARGE_FOLDER_FOOTER_DURATION` | 128 |
| `com/android/launcher3/folder/PreviewBackground.java` | `CONSUMPTION_ANIMATION_DURATION` | 100 |
| `com/android/launcher3/folder/PreviewBackground.java` | `HOVER_ANIMATION_DURATION` | 300 |
| `com/android/launcher3/folder/PreviewItemManager.java` | `INITIAL_ITEM_ANIMATION_DURATION` | 350 |
| `com/android/launcher3/folder/PreviewItemManager.java` | `FINAL_ITEM_ANIMATION_DURATION` | 200 |
| `com/android/launcher3/folder/PreviewItemManager.java` | `SLIDE_IN_FIRST_PAGE_ANIMATION_DURATION_DELAY` | 100 |
| `com/android/launcher3/folder/PreviewItemManager.java` | `SLIDE_IN_FIRST_PAGE_ANIMATION_DURATION` | 300 |
| `com/android/launcher3/graphics/AutomatedIconDelegate.kt` | `ROTATION_DURATION` | 5000L |
| `com/android/launcher3/graphics/AutomatedIconDelegate.kt` | `FIRST_ROTATION_DURATION` | 1000L |
| `com/android/launcher3/graphics/PreloadIconDelegate.kt` | `DURATION_SCALE` | 500 |
| `com/android/launcher3/graphics/PreloadIconDelegate.kt` | `SCALE_AND_ALPHA_ANIM_DURATION` | 500 |
| `com/android/launcher3/graphics/PreloadIconDelegate.kt` | `COMPLETE_ANIM_FRACTION` | 1f |
| `com/android/launcher3/keyboard/ItemFocusIndicatorHelper.java` | `ANIM_DURATION` | 150 |
| `com/android/launcher3/model/ItemInstallQueue.java` | `NEW_SHORTCUT_BOUNCE_DURATION` | 450 |
| `com/android/launcher3/model/data/FolderInfo.java` | `FLAG_MULTI_PAGE_ANIMATION` | 0 |
| `com/android/launcher3/pageindicators/PageIndicatorDots.java` | `ANIMATION_DURATION` | 200 |
| `com/android/launcher3/pageindicators/PageIndicatorDots.java` | `PAGINATION_FADE_IN_DURATION` | 83 |
| `com/android/launcher3/pageindicators/PageIndicatorDots.java` | `PAGINATION_FADE_OUT_DURATION` | 167 |
| `com/android/launcher3/pageindicators/PageIndicatorDots.java` | `ENTER_ANIMATION_START_DELAY` | 300 |
| `com/android/launcher3/pageindicators/PageIndicatorDots.java` | `ENTER_ANIMATION_STAGGERED_DELAY` | 150 |
| `com/android/launcher3/pageindicators/PageIndicatorDots.java` | `ENTER_ANIMATION_DURATION` | 400 |
| `com/android/launcher3/pageindicators/PageIndicatorDots.java` | `ENTER_ANIMATION_OVERSHOOT_TENSION` | 4 |
| `com/android/launcher3/popup/ArrowPopup.java` | `OPEN_DURATION_U` | 200 |
| `com/android/launcher3/popup/ArrowPopup.java` | `OPEN_FADE_DURATION_U` | 83 |
| `com/android/launcher3/popup/ArrowPopup.java` | `OPEN_CHILD_FADE_DURATION_U` | 83 |
| `com/android/launcher3/popup/ArrowPopup.java` | `OPEN_OVERSHOOT_DURATION_U` | 200 |
| `com/android/launcher3/popup/ArrowPopup.java` | `CLOSE_DURATION_U` | 233 |
| `com/android/launcher3/popup/ArrowPopup.java` | `CLOSE_FADE_DURATION_U` | 83 |
| `com/android/launcher3/popup/ArrowPopup.java` | `CLOSE_CHILD_FADE_DURATION_U` | 83 |
| `com/android/launcher3/popup/ui/ComposePopup.kt` | `EXPANDED_CONTENT_FADE_DURATION_RATIO` | 0 |
| `com/android/launcher3/popup/ui/ComposePopup.kt` | `COLLAPSED_CONTENT_FADE_DURATION_RATIO` | 0 |
| `com/android/launcher3/preview/PreviewSurfaceRenderer.java` | `FADE_IN_ANIMATION_DURATION` | 200 |
| `com/android/launcher3/settings/PreferenceHighlighter.java` | `HIGHLIGHT_DURATION` | 15000L |
| `com/android/launcher3/settings/PreferenceHighlighter.java` | `HIGHLIGHT_FADE_OUT_DURATION` | 500L |
| `com/android/launcher3/settings/PreferenceHighlighter.java` | `HIGHLIGHT_FADE_IN_DURATION` | 200L |
| `com/android/launcher3/settings/SettingsActivity.java` | `DELAY_HIGHLIGHT_DURATION_MILLIS` | 600 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `SKIP_ALL_ANIMATIONS` | 1 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_VERTICAL_PROGRESS` | 0 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_WORKSPACE_SCALE` | 1 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_WORKSPACE_TRANSLATE` | 2 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_WORKSPACE_FADE` | 3 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_HOTSEAT_SCALE` | 4 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_HOTSEAT_TRANSLATE` | 5 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_HOTSEAT_FADE` | 16 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_OVERVIEW_SCALE` | 6 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_OVERVIEW_TRANSLATE_X` | 7 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_OVERVIEW_TRANSLATE_Y` | 8 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_OVERVIEW_FADE` | 9 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_ALL_APPS_FADE` | 10 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_SCRIM_FADE` | 11 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_OVERVIEW_MODAL` | 12 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_DEPTH` | 13 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_OVERVIEW_ACTIONS_FADE` | 14 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_WORKSPACE_PAGE_TRANSLATE_X` | 15 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_OVERVIEW_SPLIT_SELECT_FLOATING_TASK_TRANSLATE_OFFSCREEN` | 17 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_OVERVIEW_SPLIT_SELECT_INSTRUCTIONS_FADE` | 18 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_ALL_APPS_KEYBOARD_FADE` | 19 |
| `com/android/launcher3/states/StateAnimationConfig.java` | `ANIM_TYPES_COUNT` | 21 |
| `com/android/launcher3/touch/BaseSwipeDetector.java` | `ANIMATION_DURATION` | 1200 |
| `com/android/launcher3/util/FlingBlockCheck.java` | `UNBLOCK_FLING_PAUSE_DURATION` | 200 |
| `com/android/launcher3/util/MultiTranslateDelegate.java` | `INDEX_MOVE_FROM_CENTER_ANIM` | 2 |
| `com/android/launcher3/util/MultiTranslateDelegate.java` | `INDEX_TASKBAR_ALIGNMENT_ANIM` | 3 |
| `com/android/launcher3/util/MultiTranslateDelegate.java` | `INDEX_TASKBAR_REVEAL_ANIM` | 4 |
| `com/android/launcher3/util/MultiTranslateDelegate.java` | `INDEX_TASKBAR_PINNING_ANIM` | 5 |
| `com/android/launcher3/util/MultiTranslateDelegate.java` | `INDEX_NAV_BAR_ANIM` | 6 |
| `com/android/launcher3/util/MultiTranslateDelegate.java` | `INDEX_BUBBLE_BAR_ANIM` | 7 |
| `com/android/launcher3/util/MultiTranslateDelegate.java` | `INDEX_TASKBAR_APP_RUNNING_STATE_ANIM` | 8 |
| `com/android/launcher3/util/MultiTranslateDelegate.java` | `INDEX_BUBBLE_ADJUSTMENT_ANIM` | 3 |
| `com/android/launcher3/util/WallpaperOffsetInterpolator.java` | `ANIMATION_DURATION` | 250 |
| `com/android/launcher3/util/WallpaperOffsetInterpolator.java` | `MSG_START_ANIMATION` | 1 |
| `com/android/launcher3/views/AbstractSlideInView.java` | `DEFAULT_DURATION` | 300 |
| `com/android/launcher3/views/ArrowTipView.java` | `SHOW_DURATION_MS` | 300 |
| `com/android/launcher3/views/ArrowTipView.java` | `HIDE_DURATION_MS` | 100 |
| `com/android/launcher3/views/FloatingIconView.java` | `SHAPE_PROGRESS_DURATION` | 0 |
| `com/android/launcher3/views/PredictedAppIcon.java` | `ICON_CHANGE_ANIM_DURATION` | 360 |
| `com/android/launcher3/views/PredictedAppIcon.java` | `ICON_CHANGE_ANIM_STAGGER` | 50 |
| `com/android/launcher3/views/Snackbar.java` | `SHOW_DURATION_MS` | 180 |
| `com/android/launcher3/views/Snackbar.java` | `HIDE_DURATION_MS` | 180 |
| `com/android/launcher3/views/Snackbar.java` | `TIMEOUT_DURATION_LARGE_SCREEN_MS` | 6000 |
| `com/android/launcher3/views/Snackbar.java` | `TIMEOUT_DURATION_DEFAULT_MS` | 4000 |

## C. Interpoladores y muelles

Viven repartidos en `com.android.launcher3.anim.Interpolators`, `PendingAnimation`, `SpringAnimationBuilder`, `StateAnimationConfig` y `LauncherAnimUtils`. No se han redirigido: requieren parches individuales (pendiente de M5 T5.2 completo).
