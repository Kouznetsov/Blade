package blade.mvp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.f3rog.blade.mvp.MvpActivity;

/**
 * Manages all presenters inside an App.
 */
public class PresenterManager {

    //region API

    /**
     * Removes presenters for given view.
     */
    public static void removePresentersFor(View view) {
        if (view == null) throw new IllegalArgumentException("'view' cannot be null.");

        forParentActivity(view).removeFor(view);
    }

    //endregion API

    //region INTERNAL

    /**
     * Removes presenters for given activity.
     * <p>
     * <b>NOTE:</b> Used internally by Blade library.
     * </p>
     */
    public static void removePresentersFor(Activity activity) {
        if (activity == null) throw new IllegalArgumentException("'activity' cannot be null.");

        if (!activity.isFinishing()) {
            return;
        }

        Object activityKey = buildActivityKey(activity);
        getInstance().removeActivityPresenters(activityKey);
    }

    /**
     * Stores given presenter for given view with given data.
     * <p>
     * <b>NOTE:</b> Used internally by Blade library.
     * </p>
     */
    public static <V extends IView, D> void put(V view, D data, IPresenter<V, D> presenter) {
        if (view == null) throw new IllegalArgumentException("'view' cannot be null.");
        if (data == null) throw new IllegalArgumentException("'data' cannot be null.");
        if (presenter == null) throw new IllegalArgumentException("'presenter' cannot be null.");

        if (view instanceof View) {
            View v = (View) view;
            forParentActivity(v).put(v, data, presenter);
        } else if (view instanceof Activity) {
            Activity a = (Activity) view;
            forActivity(a).put(data, presenter);
        } else {
            throw new IllegalArgumentException("View has to be instance of android View or Activity.");
        }
    }

    /**
     * Retrieves presenter of given class for given view with given data.
     * <p>
     * <b>NOTE:</b> Used internally by Blade library.
     * </p>
     */
    public static <V extends IView, D> IPresenter get(V view, D data, Class presenterClass) {
        if (view == null) throw new IllegalArgumentException("'view' cannot be null.");
        if (data == null) throw new IllegalArgumentException("'data' cannot be null.");
        if (presenterClass == null)
            throw new IllegalArgumentException("'presenter class' cannot be null.");

        if (view instanceof View) {
            View v = (View) view;
            return forParentActivity(v).get(v, data, presenterClass);
        } else if (view instanceof Activity) {
            Activity a = (Activity) view;
            return forActivity(a).get(data, presenterClass);
        } else {
            throw new IllegalArgumentException("View has to be instance of android View or Activity.");
        }
    }

    /**
     * Saves state of all presenters used in given activity into a state.
     * <p>
     * <b>NOTE:</b> Used internally by Blade library.
     * </p>
     */
    public static void savePresentersFor(Activity activity, Bundle state) {
        if (activity == null) throw new IllegalArgumentException("'activity' cannot be null.");
        if (state == null) throw new IllegalArgumentException("'state' cannot be null.");

        forActivity(activity).saveInto(state);
    }

    /**
     * Restores state of all presenters used in given activity from a state.
     * <p>
     * <b>NOTE:</b> Used internally by Blade library.
     * </p>
     */
    public static void restorePresentersFor(Activity activity, Bundle state) {
        if (activity == null) throw new IllegalArgumentException("'activity' cannot be null.");

        if (state == null) {
            return;
        }

        forActivity(activity).restoreFrom(state);
    }

    /**
     * Retrieves activity ID from a state.
     * <p>
     * <b>NOTE:</b> Used internally by Blade library.
     * </p>
     */
    public static String getActivityId(Bundle state) {
        return (state != null) ? state.getString("blade:activity_id") : UUID.randomUUID().toString();
    }

    /**
     * Stores activity ID to a state.
     * <p>
     * <b>NOTE:</b> Used internally by Blade library.
     * </p>
     */
    public static void putActivityId(Bundle state, String activityId) {
        state.putString("blade:activity_id", activityId);
    }

    //endregion INTERNAL

    //region PRIVATE

    private static PresenterManager sInstance;

    private static PresenterManager getInstance() {
        if (sInstance == null) {
            sInstance = new PresenterManager();
        }
        return sInstance;
    }

    /**
     * Returns presenter manager for parent activity of given view.
     */
    private static ActivityPresenterManager forParentActivity(View view) {
        return forActivity((Activity) view.getContext());
    }

    /**
     * Returns presenter manager for given activity.
     */
    private static ActivityPresenterManager forActivity(Activity activity) {
        Object activityKey = buildActivityKey(activity);
        return getInstance().getActivityPresenterManager(activityKey);
    }

    /**
     * Builds key for given activity. This key must ne unique for each instance.
     */
    private static Object buildActivityKey(Activity activity) {
        if (activity instanceof MvpActivity) {
            MvpActivity a = (MvpActivity) activity;
            return String.format("%s:%s", activity.getClass().getCanonicalName(), a.getId());
        } else {
            throw new IllegalStateException("Activity is missing @Blade annotation.");
        }
    }

    private final Map<Object, ActivityPresenterManager> mActivityPresenterManagers;

    private PresenterManager() {
        mActivityPresenterManagers = new HashMap<>();
    }

    /**
     * Returns presenter manager for given key.
     */
    private ActivityPresenterManager getActivityPresenterManager(Object activityKey) {
        if (!mActivityPresenterManagers.containsKey(activityKey)) {
            mActivityPresenterManagers.put(activityKey, new ActivityPresenterManager());
        }

        return mActivityPresenterManagers.get(activityKey);
    }

    /**
     * Removes presenter manager with all contained presenters for given key.
     */
    private void removeActivityPresenters(Object activityKey) {
        ActivityPresenterManager manager = mActivityPresenterManagers.remove(activityKey);
        if (manager != null) {
            manager.removeAll();
        }
    }

    /**
     * Manages all presenters inside an Activity.
     */
    private static final class ActivityPresenterManager {

        private final Map<Class, IPresenter> mActivityPresenters;
        private final Map<String, Map<Class, IPresenter>> mViewPresenters;
        private Bundle mState;

        ActivityPresenterManager() {
            mActivityPresenters = new HashMap<>();
            mViewPresenters = new HashMap<>();
            mState = null;
        }

        public <V extends IView, D> void put(View view, D data, IPresenter<V, D> presenter) {
            String viewKey = buildViewKey(view, data);
            putViewPresenter(viewKey, presenter);

            boolean restored = false;

            if (mState != null) {
                Bundle viewPresentersState = mState.getBundle(viewKey);
                if (viewPresentersState != null) {
                    String key = presenter.getClass().getCanonicalName();
                    Bundle presenterState = viewPresentersState.getBundle(key);
                    if (presenterState != null) {
                        presenter.restoreState(presenterState);
                        restored = true;
                    }
                }
            }

            presenter.create(data, restored);
        }

        public <V extends IView, D> void put(D data, IPresenter<V, D> presenter) {
            putActivityPresenter(presenter);

            boolean restored = false;

            if (mState != null) {
                String key = presenter.getClass().getCanonicalName();
                Bundle presenterState = mState.getBundle(key);
                if (presenterState != null) {
                    presenter.restoreState(presenterState);
                    restored = true;
                }
            }

            presenter.create(data, restored);
        }

        public <D> IPresenter get(View view, D data, Class presenterClass) {
            String viewKey = buildViewKey(view, data);
            return getViewPresenter(viewKey, presenterClass);
        }

        public <D> IPresenter get(D data, Class presenterClass) {
            return getActivityPresenter(presenterClass);
        }

        public void removeFor(View view) {
            Object data = view.getTag();
            if (data == null) {
                return;
            }

            String viewKey = buildViewKey(view, data);
            Map<Class, IPresenter> presenters = mViewPresenters.get(viewKey);
            if (presenters == null) {
                return;
            }

            for (IPresenter presenter : presenters.values()) {
                presenter.destroy();
            }
            presenters.clear();
            mViewPresenters.remove(viewKey);
        }

        public void removeAll() {
            // activity presenters
            for (IPresenter presenter : mActivityPresenters.values()) {
                presenter.destroy();
            }
            mActivityPresenters.clear();

            // view presenters
            for (Map<Class, IPresenter> viewPresenters : mViewPresenters.values()) {
                for (IPresenter presenter : viewPresenters.values()) {
                    presenter.destroy();
                }
                viewPresenters.clear();
            }
            mViewPresenters.clear();
        }

        public void saveInto(Bundle state) {
            // save activity presenters
            save(mActivityPresenters, state);

            // save view presenters
            for (Map.Entry<String, Map<Class, IPresenter>> viewEntry : mViewPresenters.entrySet()) {
                String viewKey = viewEntry.getKey();
                Map<Class, IPresenter> viewPresenters = viewEntry.getValue();
                Bundle viewPresentersState = new Bundle();

                save(viewPresenters, viewPresentersState);

                state.putBundle(viewKey, viewPresentersState);
            }

            mState = state;
        }

        private void save(Map<Class, IPresenter> presenters, Bundle state) {
            for (Map.Entry<Class, IPresenter> presenterEntry : presenters.entrySet()) {
                String key = presenterEntry.getKey().getCanonicalName();
                IPresenter presenter = presenterEntry.getValue();
                Bundle presenterState = new Bundle();

                presenter.saveState(presenterState);

                state.putBundle(key, presenterState);
            }
        }

        public void restoreFrom(Bundle state) {
            mState = state;
        }

        private void putViewPresenter(String viewKey, IPresenter presenter) {
            Map<Class, IPresenter> viewPresenters = mViewPresenters.get(viewKey);
            if (viewPresenters == null) {
                viewPresenters = new HashMap<>();
                mViewPresenters.put(viewKey, viewPresenters);
            }

            viewPresenters.put(presenter.getClass(), presenter);
        }

        private IPresenter getViewPresenter(String viewKey, Class presenterClass) {
            Map<Class, IPresenter> viewPresenters = mViewPresenters.get(viewKey);
            if (viewPresenters == null) {
                return null;
            }

            return viewPresenters.get(presenterClass);
        }

        private void putActivityPresenter(IPresenter presenter) {
            mActivityPresenters.put(presenter.getClass(), presenter);
        }

        private IPresenter getActivityPresenter(Class presenterClass) {
            return mActivityPresenters.get(presenterClass);
        }

        private String buildViewKey(View view, Object tagObject) {
            return String.format("%s:%s", view.getClass().getCanonicalName(), tagObject.toString());
        }
    }

    //endregion PRIVATE
}
