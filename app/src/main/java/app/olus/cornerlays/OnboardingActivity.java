package app.olus.cornerlays;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/* loaded from: classes5.dex */
public class OnboardingActivity extends AppCompatActivity {
    private Button btnFinish;
    private final ActivityResultLauncher<Intent> overlayPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback() { // from class: app.olus.cornerlays.OnboardingActivity$$ExternalSyntheticLambda0
        @Override // androidx.activity.result.ActivityResultCallback
        public final void onActivityResult(Object obj) {
            OnboardingActivity.this.lambda$new$0((ActivityResult) obj);
        }
    });

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(ActivityResult result) {
        checkOverlayPermission();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        Button btnOverlay = (Button) findViewById(R.id.btn_grant_overlay);
        Button btnAccessibility = (Button) findViewById(R.id.btn_grant_accessibility);
        this.btnFinish = (Button) findViewById(R.id.btn_finish_onboarding);
        btnOverlay.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.OnboardingActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnboardingActivity.this.lambda$onCreate$1(view);
            }
        });
        btnAccessibility.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.OnboardingActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnboardingActivity.this.lambda$onCreate$2(view);
            }
        });
        this.btnFinish.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.OnboardingActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnboardingActivity.this.lambda$onCreate$3(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(View v) {
        Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName()));
        this.overlayPermissionLauncher.launch(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(View v) {
        Intent intent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$3(View v) {
        getSharedPreferences(SettingsManager.PREFS_NAME, 0).edit().putBoolean("onboarding_complete", true).apply();
        startActivity(new Intent(this, (Class<?>) CombinedSettingsActivity.class));
        finish();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        checkOverlayPermission();
    }

    private void checkOverlayPermission() {
        boolean canDraw = Settings.canDrawOverlays(this);
        this.btnFinish.setEnabled(canDraw);
        if (canDraw) {
            this.btnFinish.setText("Weiter zur App (Berechtigt)");
        }
    }
}
