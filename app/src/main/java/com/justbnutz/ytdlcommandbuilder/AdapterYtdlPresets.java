package com.justbnutz.ytdlcommandbuilder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AdapterYtdlPresets extends RecyclerView.Adapter<AdapterYtdlPresets.ViewHolderPresetItem> {

    private final LayoutInflater mInflater;

    private List<String> mYtdlPresetList;


    AdapterYtdlPresets(Context context) {
        mInflater = LayoutInflater.from(context);
        mYtdlPresetList = new ArrayList<>();
    }


    /**
     * Refresh the list of saved Preset Commands
     */
    void updatePresetList(List<String> newPresetList, DiffUtil.DiffResult listMovements) {
        mYtdlPresetList.clear();
        mYtdlPresetList.addAll(newPresetList);
        listMovements.dispatchUpdatesTo(this);
    }


    // region ================== DEFAULT ADAPTER OVERRIDES ==================
    // ====== ================== ========================= ==================


    @NonNull
    @Override
    public ViewHolderPresetItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolderPresetItem(
                mInflater.inflate(R.layout.itemrow_ytdl_preset, parent, false)
        );
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolderPresetItem viewHolder, int position) {
        viewHolder.lblYtdlPreset
                .setText(
                        mYtdlPresetList.get(position)
                );
    }


    @Override
    public int getItemCount() {
        return (mYtdlPresetList != null)
                ? mYtdlPresetList.size()
                : 0;
    }

    // endregion


    // region ================== LIST ACTIONS ==================
    // ====== ================== ============ ==================


    List<String> getPresetList() {
        return mYtdlPresetList;
    }


    /**
     * Using DiffUtil in place of notifyDatasetChanged()
     *
     * Reference:
     * - https://medium.com/@iammert/using-diffutil-in-android-recyclerview-bdca8e4fbb00
     */
    static class PresetListDiffUtilCallback extends DiffUtil.Callback {

        List<String> mOldList;
        List<String> mNewList;

        public PresetListDiffUtilCallback(@NonNull List<String> oldList, @NonNull List<String> newList) {
            mOldList = oldList;
            mNewList = newList;
        }

        @Override
        public int getOldListSize() {
            return (mOldList != null) ? mOldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return (mNewList != null) ? mNewList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
        }
    }

    // endregion


    // region ================== VIEWHOLDERS OPS ==================
    // ====== ================== =============== ==================


    static class ViewHolderPresetItem extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        final TextView lblYtdlPreset;
        final ImageView imgCopyPreset;

        @SuppressLint("ClickableViewAccessibility")
        ViewHolderPresetItem(View itemView) {
            super(itemView);

            lblYtdlPreset = itemView.findViewById(R.id.txt_ytdl_preset);
            imgCopyPreset = itemView.findViewById(R.id.img_copy_preset);
            View fillerYtdlPreset = itemView.findViewById(R.id.view_ytdl_preset);

            lblYtdlPreset.setOnClickListener(this);
            imgCopyPreset.setOnClickListener(this);

            lblYtdlPreset.setOnLongClickListener(this);

            // Make sure the filler background View can handle the same events as the main TextView label
            fillerYtdlPreset.setOnClickListener(this);
            fillerYtdlPreset.setOnLongClickListener(this);
        }


        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.txt_ytdl_preset:
                case R.id.view_ytdl_preset:
                    HeadlessFragCommandEditor.requestReplaceYtdlCommand(
                            String.valueOf(lblYtdlPreset.getText())
                    );
                    break;

                case R.id.img_copy_preset:
                    HeadlessFragCommandCopySave.requestCopyCommand(
                            view,
                            String.valueOf(lblYtdlPreset.getText())
                    );
                    break;

                default:
                    break;
            }
        }


        @Override
        public boolean onLongClick(View view) {

            HeadlessFragSnackbar.requestPopSnackBar(
                    view,
                    view.getContext().getString(R.string.snackbar_delete_command),
                    Snackbar.LENGTH_SHORT,
                    view.getContext().getString(R.string.snackbar_confirm),
                    new SnackbarListenerItemDelete(getAdapterPosition())
            );

            return true;
        }


        /**
         * Snackbar Action handler to perform Delete action
         */
        private class SnackbarListenerItemDelete implements View.OnClickListener {

            private final int mRemovePosition;

            SnackbarListenerItemDelete(int removePosition) {
                mRemovePosition = removePosition;
            }

            @Override
            public void onClick(View view) {
                HeadlessFragPresetManager.requestRemovePresetItem(mRemovePosition);
            }
        }
    }

    // endregion
}
