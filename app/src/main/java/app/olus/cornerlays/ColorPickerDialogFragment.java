package app.olus.cornerlays;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.GridLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ColorPickerDialogFragment extends DialogFragment {

    public interface ColorPickerListener {
        void onColorSelected(int color, String tag);
    }

    private ColorPickerListener listener;

    public static ColorPickerDialogFragment newInstance(String tag) {
        ColorPickerDialogFragment fragment = new ColorPickerDialogFragment();
        Bundle args = new Bundle();
        args.putString("picker_tag", tag);
        fragment.setArguments(args);
        return fragment;
    }

    public void setColorPickerListener(ColorPickerListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        GridLayout colorGrid = (GridLayout) inflater.inflate(R.layout.dialog_color_picker, null);

        String pickerTag = getArguments().getString("picker_tag", "");

        // KORRIGIERT: Referenziert jetzt die neuen HTML-Farben
        int[] colors = {
                R.color.html_white, R.color.html_black, R.color.html_silver, R.color.html_gray,
                R.color.html_maroon, R.color.html_red, R.color.html_purple, R.color.html_fuchsia,
                R.color.html_green, R.color.html_lime, R.color.html_olive, R.color.html_yellow,
                R.color.html_navy, R.color.html_blue, R.color.html_teal, R.color.html_aqua
        };

        for (int colorRes : colors) {
            ImageButton colorSwatch = new ImageButton(getContext());
            int color = getContext().getColor(colorRes);
            colorSwatch.setImageDrawable(getContext().getDrawable(R.drawable.color_picker_swatch));
            colorSwatch.setImageTintList(ColorStateList.valueOf(color));
            colorSwatch.setBackgroundResource(R.drawable.color_picker_item_background);

            colorSwatch.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorSelected(color, pickerTag);
                }
                dismiss();
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
            colorSwatch.setLayoutParams(params);
            colorGrid.addView(colorSwatch);
        }

        builder.setView(colorGrid)
                .setTitle("Farbe auswählen")
                .setNegativeButton("Abbrechen", (dialog, id) -> dialog.cancel());

        return builder.create();
    }
}