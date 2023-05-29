package com.android.launcher3.popup;

import static com.android.launcher3.Launcher.TAG;
import static com.android.launcher3.userevent.nano.LauncherLogProto.Action;
import static com.android.launcher3.userevent.nano.LauncherLogProto.ControlType;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.util.InstantAppResolver;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.widget.WidgetsBottomSheet;

import java.util.List;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 *
 * Example system shortcuts, defined as inner classes, include Widgets and AppInfo.
 */
/**

 *表示给定应用程序的系统快捷方式。快捷方式应该有一个静态标签和图标，以及一个取决于快捷方式服务的项目的onClickListener。

 *

 *定义为内部类的示例系统快捷方式包括Widgets和AppInfo。

 */
public abstract class SystemShortcut<T extends BaseDraggingActivity> extends ItemInfo {
    public final int iconResId;
    public final int labelResId;

    public SystemShortcut(int iconResId, int labelResId) {
        this.iconResId = iconResId;
        this.labelResId = labelResId;
    }

    public abstract View.OnClickListener getOnClickListener(T activity, ItemInfo itemInfo);

    public static class Widgets extends SystemShortcut<Launcher> {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                final ItemInfo itemInfo) {
            final List<WidgetItem> widgets =
                    launcher.getPopupDataProvider().getWidgetsForPackageUser(new PackageUserKey(
                            itemInfo.getTargetComponent().getPackageName(), itemInfo.user));
            if (widgets == null) {
                return null;
            }
            return (view) -> {
                AbstractFloatingView.closeAllOpenViews(launcher);
                WidgetsBottomSheet widgetsBottomSheet =
                        (WidgetsBottomSheet) launcher.getLayoutInflater().inflate(
                                R.layout.widgets_bottom_sheet, launcher.getDragLayer(), false);
                widgetsBottomSheet.populateAndShow(itemInfo);
                launcher.getUserEventDispatcher().logActionOnControl(Action.Touch.TAP,
                        ControlType.WIDGETS_BUTTON, view);
            };
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return (view) -> {
                dismissTaskMenuView(activity);
                Rect sourceBounds = activity.getViewBounds(view);
                Bundle opts = activity.getActivityLaunchOptionsAsBundle(view);
                new PackageManagerHelper(activity).startDetailsActivityForInfo(
                        itemInfo, sourceBounds, opts);
                activity.getUserEventDispatcher().logActionOnControl(Action.Touch.TAP,
                        ControlType.APPINFO_TARGET, view);
            };
        }
    }



    public static class DeleteAPP extends SystemShortcut {
        public DeleteAPP() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return (view) -> {
                Uri uri = Uri.fromParts("package",itemInfo.getTargetComponent().getPackageName() , null);
                // 通过 itemInfo 获取当前要卸载的应用程序的包名，然后将其转换为一个 Uri 对象，
                // 该对象作为参数传入 Intent 中。
                // 具体来说，itemInfo 表示要卸载的应用程序信息，
                // getTargetComponent().getPackageName() 方法从该信息中获取应用程序的包名。
                // 然后，Uri.fromParts() 方法将应用程序的包名转换为一个 Uri 对象，
                // 其中 "package" 表示 Uri 的 scheme（即标识符），
                // itemInfo.getTargetComponent().getPackageName() 表示 Uri
                // 的 authority（即主机名），null 表示 Uri 的 fragment。
                Intent it = new Intent(Intent.ACTION_DELETE, uri);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // 创建一个 Intent 对象并指定其 Action 为 ACTION_DELETE，将上一步创建的 Uri 对象作为参数传入 Intent 中。
                // 同时为 Intent 添加一个 FLAG_ACTIVITY_NEW_TASK 标志，
                // 表示要在新的任务栈中启动 Intent。
                activity.startActivitySafely(view, it, itemInfo);//view 表示触发卸载操作的视图 View
                AbstractFloatingView.closeAllOpenViews(activity);//
                // 这一步与上面一步都是以安全方式启动intent。关闭 Launcher3 中所有开启的视图和菜单，
                // 以确保卸载操作可以正确执行。AbstractFloatingView 是一个抽象类，
                // 定义了浮动视图的基本属性和方法，其中 closeAllOpenViews() 方法用于
                // 关闭所有打开的浮动视图。
            };
        }
    }


    public static class ShareAPP extends SystemShortcut {
        public ShareAPP() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return (view) -> {
                Uri uri = Uri.fromParts("package",itemInfo.getTargetComponent().getPackageName() , null);
                Log.e(TAG, "getOnClickListener: "+uri);
                Intent sendIntent = new Intent(Intent.ACTION_SEND,uri);
//                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
//                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                activity.startActivitySafely(view, shareIntent, itemInfo);
                AbstractFloatingView.closeAllOpenViews(activity);

            };
        }
    }




    public static class Install extends SystemShortcut {
        public Install() {
            super(R.drawable.ic_install_no_shadow, R.string.install_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            boolean supportsWebUI = (itemInfo instanceof ShortcutInfo) &&
                    ((ShortcutInfo) itemInfo).hasStatusFlag(ShortcutInfo.FLAG_SUPPORTS_WEB_UI);
            boolean isInstantApp = false;
            if (itemInfo instanceof com.android.launcher3.AppInfo) {
                com.android.launcher3.AppInfo appInfo = (com.android.launcher3.AppInfo) itemInfo;
                isInstantApp = InstantAppResolver.newInstance(activity).isInstantApp(appInfo);
            }
            boolean enabled = supportsWebUI || isInstantApp;
            if (!enabled) {
                return null;
            }
            return createOnClickListener(activity, itemInfo);
        }

        public View.OnClickListener createOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return view -> {
                Intent intent = new PackageManagerHelper(view.getContext()).getMarketIntent(
                        itemInfo.getTargetComponent().getPackageName());
                activity.startActivitySafely(view, intent, itemInfo);
                AbstractFloatingView.closeAllOpenViews(activity);
            };
        }
    }

    protected static void dismissTaskMenuView(BaseDraggingActivity activity) {
        AbstractFloatingView.closeOpenViews(activity, true,
            AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);
    }
}
