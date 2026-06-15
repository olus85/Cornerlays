package app.olus.cornerlays.settings;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/* loaded from: classes6.dex */
public class SettingsFragmentAdapter extends FragmentStateAdapter {
    public SettingsFragmentAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @Override // androidx.viewpager2.adapter.FragmentStateAdapter
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new WeatherSettingsFragment();
            case 2:
                return new HaSettingsFragment();
            case 3:
                return new GeneralSettingsFragment();
            default:
                return new ClockDateSettingsFragment();
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return 4;
    }
}
