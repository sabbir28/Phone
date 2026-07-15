package s28.system.phone.ui;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.search.SearchView;
import java.util.ArrayList;
import java.util.List;
import s28.system.phone.R;
import s28.system.phone.data.ContactRepository;
import s28.system.phone.databinding.FragmentContactsBinding;
import s28.system.phone.models.Contact;
import s28.system.phone.ui.ContactsAdapter.OnContactClickListener;
import s28.system.phone.utils.PermissionManager;

public class ContactsFragment extends Fragment {
    private FragmentContactsBinding binding;
    private ContactRepository repository;
    private ContactsAdapter adapter;
    private List<Contact> allContacts = new ArrayList<>();

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
        setupSearch();
        setupAddButton();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContacts();
    }

    private void setupRecyclerView() {
        binding.rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactsAdapter(allContacts, new OnContactClickListener() {
            @Override
            public void onContactClick(Contact contact) {
                if (PermissionManager.hasAllPermissions(requireContext())) {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
                    startActivity(intent);
                } else {
                    PermissionManager.requestPermissions(requireActivity());
                }
            }

            @Override
            public void onContactEdit(Contact contact) {
                if (PermissionManager.hasAllPermissions(requireContext())) {
                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.getId());
                    intent.setData(contactUri);
                    intent.putExtra("finishActivityOnSaveCompleted", true);
                    startActivity(intent);
                } else {
                    PermissionManager.requestPermissions(requireActivity());
                }
            }
        });
        binding.rvContacts.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void setupAddButton() {
        binding.fabAddContact.setOnClickListener(v -> {
            if (PermissionManager.hasAllPermissions(requireContext())) {
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                intent.putExtra(ContactsContract.Intents.Insert.NAME, "");
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, "");
                startActivity(intent);
            } else {
                PermissionManager.requestPermissions(requireActivity());
            }
        });
    }

    private void refreshContacts() {
        if (!PermissionManager.hasAllPermissions(requireContext())) {
            PermissionManager.requestPermissions(requireActivity());
            return;
        }

        allContacts.clear();
        allContacts.addAll(repository.getAllContacts());
        adapter.updateContacts(allContacts);
    }
}
