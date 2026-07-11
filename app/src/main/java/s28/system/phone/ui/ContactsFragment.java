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
import s28.system.phone.data.ContactRepository;
import s28.system.phone.databinding.FragmentContactsBinding;
import s28.system.phone.models.Contact;
import s28.system.phone.utils.PermissionManager;

public class ContactsFragment extends Fragment {
    private FragmentContactsBinding binding;
    private ContactRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ContactRepository(requireContext());
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        if (PermissionManager.hasAllPermissions(requireContext())) {
            binding.rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
            ContactsAdapter adapter = new ContactsAdapter(repository.getAllContacts(), contact -> {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
                startActivity(intent);
            });
            binding.rvContacts.setAdapter(adapter);
        }
    }
}
