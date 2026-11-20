package local.notion.pinnedpage;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Method;
import java.util.UUID;

public final class NotionPinnedPageModule extends XposedModule {
    private static final String PACKAGE_NAME = "notion.id";
    private static final String MAIN_ACTIVITY = "notion.local.id.MainActivity";
    private static final String TAG = "NotionPinnedPage";
    private static final String FORWARDED_PIN = "local.notion.pinnedpage.FORWARDED_PIN";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int navigationGeneration;

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!PACKAGE_NAME.equals(param.getPackageName()) || !param.isFirstPackage()) {
            return;
        }

        Class<?> activity;
        try {
            activity = Class.forName(MAIN_ACTIVITY, false, param.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            Log.w(TAG, "Notion's main activity was not found");
            return;
        }

        hookLifecycle(activity, "onMAMCreate", false, Bundle.class);
        hookLifecycle(activity, "onMAMNewIntent", true, Intent.class);
        try {
            hook(activity.getDeclaredMethod("onBackPressed")).intercept(chain -> {
                navigationGeneration++;
                return chain.proceed();
            });
        } catch (NoSuchMethodException ignored) {
            Log.w(TAG, "Notion's back callback was not found");
        }
        try {
            hook(Activity.class.getDeclaredMethod("dispatchTouchEvent", MotionEvent.class))
                    .intercept(chain -> {
                MotionEvent event = (MotionEvent) chain.getArg(0);
                if (activity.isInstance(chain.getThisObject())
                        && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    navigationGeneration++;
                }
                return chain.proceed();
            });
        } catch (NoSuchMethodException ignored) {
            Log.w(TAG, "Touch dispatch method was not found");
        }
    }

    private void hookLifecycle(Class<?> activity, String name, boolean hasIntentArgument,
            Class<?>... parameterTypes) {
        Method method;
        try {
            method = activity.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            Log.w(TAG, "Lifecycle method was not found: " + name);
            return;
        }

        hook(method).intercept(chain -> {
            Activity receiver = (Activity) chain.getThisObject();
            Intent intent = hasIntentArgument ? (Intent) chain.getArg(0) : receiver.getIntent();
            String link = findPinnedPageLink(intent);
            if (link == null) {
                if (hasIntentArgument && intent != null && !intent.hasExtra(FORWARDED_PIN)
                        && (intent.getData() != null || intent.hasCategory(Intent.CATEGORY_LAUNCHER))) {
                    navigationGeneration++;
                }
                return chain.proceed();
            }

            int generation = ++navigationGeneration;
            Uri destination = Uri.parse(link);
            intent.removeExtra("source");
            intent.removeExtra("url");
            if (hasIntentArgument) {
                intent.setData(destination);
                // Use the same bookkeeping as Notion's app-link entry point.
                intent.putExtra("routeDispatchId", UUID.randomUUID().toString());
                Log.d(TAG, "Converted a pinned page launch in the running app");
                Object result = chain.proceed();
                scheduleRetry(receiver, destination, generation, 2100);
                return result;
            }

            // Let Notion initialize normally before delivering its app link.
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setSourceBounds(null);
            Object result = chain.proceed();
            mainHandler.postDelayed(() -> {
                if (canContinue(receiver, generation) && openLink(receiver, destination)) {
                    scheduleRetry(receiver, destination, generation, 4900);
                }
            }, 1300);
            return result;
        });
    }

    private static String findPinnedPageLink(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())
                || intent.getData() != null || intent.getType() != null) {
            return null;
        }

        try {
            return PinnedPageLink.fromShortcut(
                    intent.getStringExtra("source"), intent.getStringExtra("url"));
        } catch (BadParcelableException | ClassCastException ignored) {
            return null;
        }
    }

    private boolean canContinue(Activity activity, int generation) {
        return generation == navigationGeneration && !activity.isFinishing()
                && !activity.isDestroyed() && activity.hasWindowFocus();
    }

    private void scheduleRetry(Activity activity, Uri destination, int generation, long delayMillis) {
        mainHandler.postDelayed(
                () -> retryWhenFocused(activity, destination, generation, 6), delayMillis);
    }

    private void retryWhenFocused(Activity activity, Uri destination, int generation, int focusChecks) {
        if (generation != navigationGeneration) {
            Log.d(TAG, "The pinned page check was superseded");
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.d(TAG, "The Notion activity closed before the pinned page check");
            return;
        }
        if (!activity.hasWindowFocus()) {
            if (focusChecks > 0) {
                mainHandler.postDelayed(
                        () -> retryWhenFocused(activity, destination, generation, focusChecks - 1), 450);
            }
            return;
        }
        // A workspace switch can consume the initial navigation without opening the page.
        if (openLink(activity, destination)) {
            Log.d(TAG, "Retried the pinned page once after navigation settled");
        }
    }

    private static boolean openLink(Activity activity, Uri link) {
        Intent destination = new Intent(Intent.ACTION_VIEW, link).setPackage(PACKAGE_NAME)
                .putExtra(FORWARDED_PIN, true);
        try {
            activity.startActivity(destination);
            Log.d(TAG, "Forwarded a pinned page through the Notion app-link handler");
            return true;
        } catch (ActivityNotFoundException ignored) {
            Log.w(TAG, "No Notion activity handles the pinned page link");
            return false;
        }
    }
}
