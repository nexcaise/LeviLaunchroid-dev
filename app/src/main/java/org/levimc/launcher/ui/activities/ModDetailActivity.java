package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.core.view.ViewCompat;

import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.lifecycle.ViewModelProvider;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.io.InputStream;

import java.util.HashMap;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import android.widget.ImageView;

public class ModDetailActivity extends BaseActivity {

    private MainViewModel viewModel;
    private Mod currentMod;
    private int modPosition;
    private TextView modNameText;
    private TextView modFilenameText;
    private TextView modOrderText;
    private TextView modVersionText;
    private TextView modAuthorText;
    private TextView modDescriptionText;

    private String modFilenameArg;
    private View headerContainer;
    private View infoContainer;
    private View actionsContainer;
    private ImageView icon;
    
    private final HashMap<String, Bitmap> iconCache = new HashMap<>();
    private Bitmap defaultIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_detail);

        View root = findViewById(R.id.mod_detail_root);
        if (root != null) {
            DynamicAnim.applyPressScaleRecursively(root);
        }

        if (getIntent().hasExtra("mod_filename") && getIntent().hasExtra("mod_position")) {
            modFilenameArg = getIntent().getStringExtra("mod_filename");
            modPosition = getIntent().getIntExtra("mod_position", -1);
            
            setupViewModel();
            setupViews();
            runEnterAnimations();
            
            loadModDetails(modFilenameArg);
        } else {
            Toast.makeText(this, R.string.error_loading_mod, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);
    }

    private void setupViews() {
        modNameText = findViewById(R.id.mod_name_detail);
        modFilenameText = findViewById(R.id.mod_filename_detail);
        modOrderText = findViewById(R.id.mod_order_detail);
        
        modVersionText = findViewById(R.id.mod_version_detail);
        modAuthorText = findViewById(R.id.mod_author_detail);
        modDescriptionText = findViewById(R.id.mod_description_detail);
        
        icon = findViewById(R.id.mod_icon);
        headerContainer = findViewById(R.id.mod_detail_header_container);
        infoContainer = findViewById(R.id.mod_detail_info_container);
        actionsContainer = findViewById(R.id.mod_detail_actions_container);

        // 与列表共享元素匹配：使用最外层头部卡片作为共享元素
        if (modFilenameArg != null && headerContainer != null) {
            ViewCompat.setTransitionName(headerContainer, "mod_card_" + modFilenameArg);
        }

        ImageButton closeButton = findViewById(R.id.close_detail_button);
        closeButton.setOnClickListener(v -> finish());
        DynamicAnim.applyPressScale(closeButton);

        Button deleteButton = findViewById(R.id.delete_mod_button);
        deleteButton.setOnClickListener(v -> confirmDeleteMod());
        DynamicAnim.applyPressScale(deleteButton);

        DynamicAnim.applyPressScale(modSwitch);
    }

    private void loadModDetails(String modFilename) {
        if (viewModel != null) {
            viewModel.getModsLiveData().observe(this, mods -> {
                if (mods != null) {
                    for (Mod mod : mods) {
                        if (mod.getFileName().equals(modFilename)) {
                            currentMod = mod;
                            updateModUI(mod);
                            break;
                        }
                    }
                }
            });
            
            viewModel.refreshMods();
        }
    }

    private void updateModUI(Mod mod) {
        if (mod != null) {
            modNameText.setText(mod.getDisplayName());
            modFilenameText.setText(getString(R.string.mod_filename_format, mod.getFileName()));
            modOrderText.setText(getString(R.string.mod_load_order, modPosition + 1));
            modDescriptionText.setText(getString(R.string.mod_description) + mod.getDescription());
            modAuthorText.setText(getString(R.string.mod_author) + mod.getAuthor());
            modVersionText.setText(getString(R.string.mod_version) + mod.getVersion());
            if (icon != null) {
                loadModIcon(icon.getContext(), mod, icon);
            }
        }
    }

    private void runEnterAnimations() {
        float density = getResources().getDisplayMetrics().density;
        float dy = 16f * density;

        View[] cards = new View[]{headerContainer, infoContainer, actionsContainer};
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            if (card == null) continue;
            card.setAlpha(0f);
            card.setTranslationY(dy);
            final int delay = 100 + i * 80;
            card.postDelayed(() -> {
                DynamicAnim.springAlphaTo(card, 1f).start();
                DynamicAnim.springTranslationYTo(card, 0f).start();
            }, delay);
        }

        // 不对 mod 名称做入场动画，保持静态
    }

    private void confirmDeleteMod() {
        if (currentMod != null) {
            new CustomAlertDialog(this)
                    .setTitleText(getString(R.string.dialog_title_delete_mod))
                    .setMessage(getString(R.string.dialog_message_delete_mod))
                    .setPositiveButton(getString(R.string.dialog_positive_delete), v -> {
                        viewModel.removeMod(currentMod);
                        Toast.makeText(this, R.string.delete_mod, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                    .show();
        }
    }

    private Bitmap loadIconFromLLMod(Mod mod) {
    
        try {
    
            File modFile = new File(
                ModManager.getInstance().getCurrentVersion().modsDir,
                mod.getFileName()
            );
    
            ZipFile zip = new ZipFile(modFile);
    
            ZipEntry entry = zip.getEntry(mod.getIconName());
    
            if (entry != null) {
    
                InputStream is = zip.getInputStream(entry);
                Bitmap bmp = BitmapFactory.decodeStream(is);
    
                zip.close();
                return bmp;
            }
    
            zip.close();
    
        } catch (Exception ignored) {}
    
        return null;
    }

    private void loadModIcon(Context context, Mod mod, ImageView imageView) {

        String key = mod.getFileName();

        // cache hit
        if (iconCache.containsKey(key)) {
            imageView.setImageBitmap(iconCache.get(key));
            return;
        }

        Bitmap iconBitmap = loadIconFromLLMod(mod);

        // fallback ke default icon
        if (iconBitmap == null) {

            try {

                if (defaultIcon == null) {
                    InputStream is = context.getAssets().open("default_mod_icon.png");
                    defaultIcon = BitmapFactory.decodeStream(is);
                }

                iconBitmap = defaultIcon;

            } catch (Exception ignored) {}
        }

        if (iconBitmap != null) {
            iconCache.put(key, iconBitmap);
            imageView.setImageBitmap(iconBitmap);
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }

    // Exit animation is now handled uniformly in BaseActivity.finish()
}