package com.simplecity.amp_library.ui.drawer;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;
import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.bignerdranch.expandablerecyclerview.model.Parent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SleepTimer;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.TypefaceManager;
import com.simplecityapps.recycler_adapter.recyclerview.AttachStateViewHolder;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class DrawerParent implements Parent<DrawerChild> {

    private static final String TAG = "DrawerParent";

    static DrawerParent libraryParent = new DrawerParent(DrawerParent.Type.LIBRARY, R.string.library_title, R.drawable.ic_library_white, NavigationEventRelay.librarySelectedEvent, true);
    static DrawerParent folderParent = new DrawerParent(DrawerParent.Type.FOLDERS, R.string.folders_title, R.drawable.ic_folders_many_white, NavigationEventRelay.foldersSelectedEvent, true);
    static DrawerParent playlistsParent = new DrawerParent(DrawerParent.Type.PLAYLISTS, R.string.playlists_title, R.drawable.ic_action_toggle_queue, null, true);
    static DrawerParent sleepTimerParent = new DrawerParent(Type.SLEEP_TIMER, R.string.sleep_timer, R.drawable.ic_sleep_24dp, NavigationEventRelay.sleepTimerSelectedEvent, false);
    static DrawerParent equalizerParent = new DrawerParent(Type.EQUALIZER, R.string.equalizer, R.drawable.ic_equalizer_24dp, NavigationEventRelay.equalizerSelectedEvent, false);
    static DrawerParent settingsParent = new DrawerParent(DrawerParent.Type.SETTINGS, R.string.settings, R.drawable.ic_action_settings, NavigationEventRelay.settingsSelectedEvent, false);
    static DrawerParent supportParent = new DrawerParent(DrawerParent.Type.SUPPORT, R.string.pref_title_support, R.drawable.ic_settings_help, NavigationEventRelay.supportSelectedEvent, false);

    public @interface Type {
        int LIBRARY = 0;
        int FOLDERS = 1;
        int PLAYLISTS = 2;
        int SLEEP_TIMER = 3;
        int EQUALIZER = 4;
        int SETTINGS = 5;
        int SUPPORT = 6;
    }

    boolean selectable = true;

    public interface ClickListener {
        void onClick(DrawerParent drawerParent);
    }

    @Nullable
    private ClickListener listener;

    public void setListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    @DrawerParent.Type
    public int type;

    @Nullable NavigationEventRelay.NavigationEvent navigationEvent;

    @StringRes private int titleResId;

    @DrawableRes private int iconResId;

    List<DrawerChild> children = new ArrayList<>();

    private boolean isSelected;

    DrawerParent(@DrawerParent.Type int type, int titleResId, int iconResId, @Nullable NavigationEventRelay.NavigationEvent navigationEvent, boolean selectable) {
        this.type = type;
        this.titleResId = titleResId;
        this.iconResId = iconResId;
        this.navigationEvent = navigationEvent;
        this.selectable = selectable;
    }

    @Override
    public List<DrawerChild> getChildList() {
        return children;
    }

    @Override
    public boolean isInitiallyExpanded() {
        return false;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    void onClick() {
        if (listener != null && type != Type.PLAYLISTS) {
            listener.onClick(this);
        }
    }

    Drawable getDrawable(Context context) {
        Drawable drawable = DrawableCompat.wrap(context.getResources().getDrawable(iconResId));
        DrawableCompat.setTint(drawable, isSelected ? Aesthetic.get().colorPrimary().blockingFirst() : Aesthetic.get().textColorPrimary().blockingFirst());
        return drawable;
    }

    public void bindView(ParentHolder holder) {

        holder.bind(this);

        int imageResourceId = holder.isExpanded() ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down;
        holder.expandableIcon.setImageDrawable(holder.itemView.getResources().getDrawable(imageResourceId));
        holder.expandableIcon.setVisibility(getChildList().isEmpty() ? View.GONE : View.VISIBLE);

        holder.icon.setImageDrawable(getDrawable(holder.itemView.getContext()));
        if (iconResId != -1) {
            holder.icon.setVisibility(View.VISIBLE);
        } else {
            holder.icon.setVisibility(View.GONE);
        }

        if (titleResId != -1) {
            holder.lineOne.setText(holder.itemView.getResources().getString(titleResId));
            holder.lineOne.setTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF_MEDIUM));
        }

        if (isSelected) {
            holder.itemView.setActivated(true);
        } else {
            holder.itemView.setActivated(false);
            holder.icon.setAlpha(0.6f);
        }

        if (type == DrawerParent.Type.FOLDERS && !ShuttleUtils.isUpgraded()) {
            holder.itemView.setAlpha(0.4f);
            holder.itemView.setEnabled(false);
        } else {
            holder.itemView.setEnabled(true);
            holder.itemView.setAlpha(1.0f);
        }

        if (type == DrawerParent.Type.PLAYLISTS) {
            holder.itemView.setAlpha(getChildList().isEmpty() ? 0.4f : 1.0f);
            holder.itemView.setEnabled(!getChildList().isEmpty());
        }
    }

    static class ParentHolder extends ParentViewHolder implements AttachStateViewHolder {

        private DrawerParent drawerParent;

        private CompositeSubscription subscriptions = new CompositeSubscription();

        @BindView(R.id.icon)
        ImageView icon;

        @BindView(R.id.line_one)
        TextView lineOne;

        @BindView(R.id.expandable_icon)
        ImageView expandableIcon;

        @BindView(R.id.timeRemaining)
        TextView timeRemaining;

        private ObjectAnimator objectAnimator;

        ParentHolder(@NonNull View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        void bind(DrawerParent drawerParent) {
            this.drawerParent = drawerParent;
        }

        @Override
        public void onExpansionToggled(boolean expanded) {
            super.onExpansionToggled(expanded);

            if (objectAnimator != null) {
                objectAnimator.cancel();
            }

            objectAnimator = ObjectAnimator.ofFloat(expandableIcon, View.ROTATION,
                    expanded ? expandableIcon.getRotation() : expandableIcon.getRotation(),
                    expanded ? 0f : -180f);
            objectAnimator.setDuration(250);
            objectAnimator.setStartDelay(expanded ? 100 : 0);
            objectAnimator.setInterpolator(new DecelerateInterpolator(1.2f));
            objectAnimator.start();
        }

        @Override
        public void onClick(View v) {
            super.onClick(v);

            drawerParent.onClick();
        }

        @Override
        public void onAttachedToWindow() {
            if (drawerParent.type == Type.SLEEP_TIMER) {
                subscriptions.add(SleepTimer.getInstance().getCurrentTimeObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aLong -> {
                            if (aLong > 0) {
                                timeRemaining.setText(StringUtils.makeTimeString(itemView.getContext(), aLong));
                            }
                        }, throwable -> LogUtils.logException("DrawerParent error observing sleep time", throwable)));

                subscriptions.add(SleepTimer.getInstance().getTimerActiveSubject()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(active -> timeRemaining.setVisibility(active ? View.VISIBLE : View.GONE),
                                throwable -> LogUtils.logException("DrawerParent error observing sleep state", throwable))
                );
            }
        }

        @Override
        public void onDetachedFromWindow() {
            if (subscriptions != null) {
                subscriptions.clear();
            }
        }
    }
}