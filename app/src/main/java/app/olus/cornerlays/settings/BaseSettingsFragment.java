package app.olus.cornerlays.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import androidx.fragment.app.Fragment;
import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;

/* loaded from: classes6.dex */
public abstract class BaseSettingsFragment extends Fragment {
    protected SharedPreferences sharedPreferences;

    @Override // androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        this.sharedPreferences = context.getSharedPreferences(SettingsManager.PREFS_NAME, 0);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public SharedPreferences.Editor getEditor() {
        return this.sharedPreferences.edit();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void restartServiceIfRunning(final Class<?> serviceClass, String enabledKey) {
        if (this.sharedPreferences.getBoolean(enabledKey, false)) {
            requireActivity().stopService(new Intent(getActivity(), serviceClass));
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // from class: app.olus.cornerlays.settings.BaseSettingsFragment$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BaseSettingsFragment.this.lambda$restartServiceIfRunning$0(serviceClass);
                }
            }, 200L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$restartServiceIfRunning$0(Class serviceClass) {
        if (getActivity() != null) {
            requireActivity().startService(new Intent(getActivity(), (Class<?>) serviceClass));
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void loadPositionSpinner(Spinner spinner, String gravityKey) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.position_options_array, R.layout.custom_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
        int gravity = this.sharedPreferences.getInt(gravityKey, 8388661);
        spinner.setSelection(getSpinnerIndexForGravity(gravity));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getGravityForSpinnerIndex(int index) {
        switch (index) {
            case 1:
                return 8388659;
            case 2:
                return 8388693;
            case 3:
                return 8388691;
            default:
                return 8388661;
        }
    }

    protected int getSpinnerIndexForGravity(int gravity) {
        if (gravity == 8388659) {
            return 1;
        }
        if (gravity == 8388693) {
            return 2;
        }
        return gravity == 8388691 ? 3 : 0;
    }
}
