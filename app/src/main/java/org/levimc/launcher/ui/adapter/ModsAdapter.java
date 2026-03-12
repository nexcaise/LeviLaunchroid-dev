package org.levimc.launcher.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;

import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.Mod;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ModViewHolder> {

    private List<Mod> mods = new ArrayList<>();

    private OnModEnableChangeListener onModEnableChangeListener;
    private OnModReorderListener onModReorderListener;
    private OnModClickListener onModClickListener;

    // icon cache
    private final HashMap<String, Bitmap> iconCache = new HashMap<>();
    private Bitmap defaultIcon;

    public interface OnModEnableChangeListener {
        void onEnableChanged(Mod mod, boolean enabled);
    }

    public interface OnModReorderListener {
        void onModsReordered(List<Mod> reorderedMods);
    }

    public interface OnModClickListener {
        void onModClick(Mod mod, int position, View sharedView);
    }

    public ModsAdapter(List<Mod> mods) {
        this.mods = mods;
    }

    public void setOnModEnableChangeListener(OnModEnableChangeListener l) {
        this.onModEnableChangeListener = l;
    }

    public void setOnModReorderListener(OnModReorderListener listener) {
        this.onModReorderListener = listener;
    }

    public void setOnModClickListener(OnModClickListener listener) {
        this.onModClickListener = listener;
    }

    @NonNull
    @Override
    public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mod, parent, false);
        return new ModViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {

        Mod mod = mods.get(position);

        holder.name.setText(mod.getDisplayName());
        holder.description.setText(mod.getDescription());
        holder.version.setText(mod.getVersion());

        if (holder.orderText != null) {
            holder.orderText.setText(
                    holder.itemView.getContext()
                            .getString(R.string.mod_load_order, position + 1)
            );
            holder.orderText.setVisibility(View.VISIBLE);
        }

        // load icon
        if (holder.icon != null) {
            loadModIcon(holder.icon.getContext(), mod, holder.icon);
        }

        holder.switchBtn.setOnCheckedChangeListener(null);
        holder.switchBtn.setChecked(mod.isEnabled());

        holder.switchBtn.setOnCheckedChangeListener((btn, isChecked) -> {

            if (isChecked == mod.isEnabled()) return;

            mod.setEnabled(isChecked);

            if (onModEnableChangeListener != null) {
                onModEnableChangeListener.onEnableChanged(mod, isChecked);
            }
        });

        // shared element transition
        ViewCompat.setTransitionName(holder.itemView, "mod_card_" + mod.getFileName());

        holder.itemView.setOnClickListener(v -> {
            if (onModClickListener != null) {
                onModClickListener.onModClick(mod, position, holder.itemView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mods != null ? mods.size() : 0;
    }

    public Mod getItem(int pos) {
        return mods.get(pos);
    }

    public void removeAt(int pos) {
        mods.remove(pos);
        notifyItemRemoved(pos);
    }

    public void updateMods(List<Mod> list) {
        this.mods = list;
        notifyDataSetChanged();
    }

    public void moveItem(int fromPosition, int toPosition) {

        if (mods == null || mods.isEmpty()) return;
        if (fromPosition < 0 || toPosition < 0) return;
        if (fromPosition >= mods.size() || toPosition >= mods.size()) return;
        if (fromPosition == toPosition) return;

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mods, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mods, i, i - 1);
            }
        }

        notifyItemMoved(fromPosition, toPosition);

        if (onModReorderListener != null) {
            onModReorderListener.onModsReordered(new ArrayList<>(mods));
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

    static class ModViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView name;
        TextView description;
        TextView version;
        TextView orderText;
        Switch switchBtn;

        public ModViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.mod_icon);
            name = itemView.findViewById(R.id.mod_name);
            description = itemView.findViewById(R.id.mod_description);
            version = itemView.findViewById(R.id.mod_version);
            orderText = itemView.findViewById(R.id.mod_order);
            switchBtn = itemView.findViewById(R.id.mod_switch);
        }
    }
}