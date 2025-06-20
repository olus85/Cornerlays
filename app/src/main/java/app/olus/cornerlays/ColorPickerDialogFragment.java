package app.olus.cornerlays;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

public class ColorPickerDialogFragment extends DialogFragment {

    public interface ColorPickerListener {
        void onColorSelected(int color, String tag);
    }

    private ColorPickerListener listener;
    private static final String ARG_TAG = "dialog_tag";

    public static ColorPickerDialogFragment newInstance(String tag) {
        ColorPickerDialogFragment fragment = new ColorPickerDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAG, tag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getTargetFragment() instanceof ColorPickerListener) {
            listener = (ColorPickerListener) getTargetFragment();
        } else {
            try {
                listener = (ColorPickerListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " or target fragment must implement ColorPickerListener");
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_color_picker, null);
        GridLayout gridLayout = view.findViewById(R.id.color_grid);

        int[] colors = getResources().getIntArray(R.array.color_palette);

        for (final int color : colors) {
            View colorView = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = (int) getResources().getDimension(R.dimen.color_swatch_size);
            params.height = (int) getResources().getDimension(R.dimen.color_swatch_size);
            params.setMargins(10, 10, 10, 10);
            colorView.setLayoutParams(params);

            // KORRIGIERT: Hintergrund und Vordergrund (Fokus) getrennt setzen
            GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.color_swatch_background).mutate();
            background.setColor(color);
            colorView.setBackground(background);
            colorView.setForeground(ContextCompat.getDrawable(requireContext(), R.drawable.color_swatch_foreground));

            colorView.setFocusable(true);

            colorView.setOnClickListener(v -> {
                if (listener != null) {
                    String tag = getArguments().getString(ARG_TAG);
                    listener.onColorSelected(color, tag);
                }
                dismiss();
            });
            gridLayout.addView(colorView);
        }

        builder.setView(view).setTitle("Farbe wählen");
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            GridLayout gridLayout = dialog.findViewById(R.id.color_grid);
            if (gridLayout != null && gridLayout.getChildCount() > 0) {
                gridLayout.getChildAt(0).requestFocus();
            }
        }
    }

    public void setColorPickerListener(ColorPickerListener listener) {
        this.listener = listener;
    }
}