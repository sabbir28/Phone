package s28.system.phone.ui;

import android.media.MediaPlayer;
import android.provider.CallLog;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import s28.system.phone.R;
import s28.system.phone.data.RecordingRepository;
import s28.system.phone.databinding.ItemCallLogBinding;
import s28.system.phone.models.CallLogItem;
import s28.system.phone.models.RecordingItem;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder> {
    private final List<CallLogItem> logs;
    private final OnCallLogClickListener listener;
    private MediaPlayer mediaPlayer;
    private String currentlyPlayingPath;
    private View currentlyPlayingButton;

    public interface OnCallLogClickListener {
        void onCallLogClick(CallLogItem item);
    }

    public CallLogAdapter(List<CallLogItem> logs, OnCallLogClickListener listener) {
        this.logs = logs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CallLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCallLogBinding binding = ItemCallLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CallLogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CallLogViewHolder holder, int position) {
        holder.bind(logs.get(position));
    }

    @Override
    public int getItemCount() { return logs.size(); }

    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    class CallLogViewHolder extends RecyclerView.ViewHolder {
        private final ItemCallLogBinding binding;
        private final RecordingRepository recordingRepository;

        public CallLogViewHolder(ItemCallLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.recordingRepository = new RecordingRepository(binding.getRoot().getContext());
        }

        public void bind(CallLogItem item) {
            String displayName = (item.getName() != null && !item.getName().isEmpty()) ? item.getName() : item.getNumber();
            binding.tvCallerName.setText(displayName);
            
            String dateStr = DateUtils.getRelativeTimeSpanString(item.getDate()).toString();
            binding.tvCallDetails.setText(item.getNumber() + " • " + dateStr);

            int iconRes;
            switch (item.getType()) {
                case CallLog.Calls.OUTGOING_TYPE:
                    iconRes = android.R.drawable.sym_call_outgoing;
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    iconRes = android.R.drawable.sym_call_missed;
                    break;
                default:
                    iconRes = android.R.drawable.sym_call_incoming;
                    break;
            }
            binding.ivCallType.setImageResource(iconRes);

            binding.btnCallAction.setOnClickListener(v -> listener.onCallLogClick(item));

            // Expand/Collapse logic
            binding.getRoot().setOnClickListener(v -> {
                item.setExpanded(!item.isExpanded());
                notifyItemChanged(getAdapterPosition());
            });

            if (item.isExpanded()) {
                List<RecordingItem> recordings = recordingRepository.getRecordingsForNumber(item.getNumber());
                if (!recordings.isEmpty()) {
                    binding.recordingsContainer.setVisibility(View.VISIBLE);
                    setupRecordingsList(recordings);
                } else {
                    binding.recordingsContainer.setVisibility(View.GONE);
                }
            } else {
                binding.recordingsContainer.setVisibility(View.GONE);
            }
        }

        private void setupRecordingsList(List<RecordingItem> recordings) {
            binding.llRecordingsList.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

            for (RecordingItem recording : recordings) {
                View view = inflater.inflate(R.layout.item_recording_mini, binding.llRecordingsList, false);
                TextView tvTime = view.findViewById(R.id.tvRecordingTime);
                View btnPlay = view.findViewById(R.id.btnPlayStop);

                tvTime.setText(sdf.format(new Date(recording.getTimestamp())));
                
                // Update button icon if this is the one playing
                if (recording.getFilePath().equals(currentlyPlayingPath)) {
                    ((com.google.android.material.button.MaterialButton)btnPlay).setIconResource(android.R.drawable.ic_media_pause);
                    currentlyPlayingButton = btnPlay;
                } else {
                    ((com.google.android.material.button.MaterialButton)btnPlay).setIconResource(android.R.drawable.ic_media_play);
                }

                btnPlay.setOnClickListener(v -> togglePlayback(recording, (com.google.android.material.button.MaterialButton)v));
                binding.llRecordingsList.addView(view);
            }
        }

        private void togglePlayback(RecordingItem recording, com.google.android.material.button.MaterialButton button) {
            if (recording.getFilePath().equals(currentlyPlayingPath)) {
                stopPlayback();
            } else {
                startPlayback(recording, button);
            }
        }

        private void startPlayback(RecordingItem recording, com.google.android.material.button.MaterialButton button) {
            stopPlayback();

            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(recording.getFilePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
                currentlyPlayingPath = recording.getFilePath();
                currentlyPlayingButton = button;
                button.setIconResource(android.R.drawable.ic_media_pause);

                mediaPlayer.setOnCompletionListener(mp -> stopPlayback());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void stopPlayback() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (currentlyPlayingButton != null) {
                ((com.google.android.material.button.MaterialButton)currentlyPlayingButton).setIconResource(android.R.drawable.ic_media_play);
            }
            currentlyPlayingPath = null;
            currentlyPlayingButton = null;
        }
    }
}
