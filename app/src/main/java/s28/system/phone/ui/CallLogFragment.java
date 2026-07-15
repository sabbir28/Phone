package s28.system.phone.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import s28.system.phone.data.CallLogRepository;
import s28.system.phone.databinding.FragmentCallLogBinding;
import s28.system.phone.models.CallLogItem;
import s28.system.phone.utils.PermissionManager;

public class CallLogFragment extends Fragment {
    private FragmentCallLogBinding binding;
    private CallLogRepository repository;

    private CallLogAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCallLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new CallLogRepository(requireContext());
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        if (PermissionManager.hasAllPermissions(requireContext())) {
            binding.rvCallLog.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new CallLogAdapter(repository.getCallLogs(), item -> {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + item.getNumber()));
                startActivity(intent);
            });
            binding.rvCallLog.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.releaseMediaPlayer();
        }
    }
}
