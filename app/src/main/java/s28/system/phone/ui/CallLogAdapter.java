package s28.system.phone.ui;

import android.provider.CallLog;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import s28.system.phone.databinding.ItemCallLogBinding;
import s28.system.phone.models.CallLogItem;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder> {
    private final List<CallLogItem> logs;
    private final OnCallLogClickListener listener;

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

    class CallLogViewHolder extends RecyclerView.ViewHolder {
        private final ItemCallLogBinding binding;

        public CallLogViewHolder(ItemCallLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
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
        }
    }
}
