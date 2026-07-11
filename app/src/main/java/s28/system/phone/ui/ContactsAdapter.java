package s28.system.phone.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import java.util.ArrayList;
import java.util.List;
import s28.system.phone.R;
import s28.system.phone.databinding.ItemContactBinding;
import s28.system.phone.models.Contact;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
    private final List<Contact> contacts;
    private final List<Contact> filteredContacts;
    private final OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
        void onContactEdit(Contact contact);
    }

    public ContactsAdapter(List<Contact> contacts, OnContactClickListener listener) {
        this.contacts = new ArrayList<>(contacts);
        this.filteredContacts = new ArrayList<>(contacts);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactBinding binding = ItemContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ContactViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        holder.bind(filteredContacts.get(position));
    }

    @Override
    public int getItemCount() { return filteredContacts.size(); }

    public void updateContacts(List<Contact> contacts) {
        this.contacts.clear();
        this.contacts.addAll(contacts);
        filter(null);
    }

    public void filter(String query) {
        filteredContacts.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredContacts.addAll(contacts);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Contact contact : contacts) {
                if (contact.getName().toLowerCase().contains(lowerQuery)
                        || contact.getPhoneNumber().toLowerCase().contains(lowerQuery)) {
                    filteredContacts.add(contact);
                }
            }
        }
        notifyDataSetChanged();
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactBinding binding;

        public ContactViewHolder(ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Contact contact) {
            binding.tvContactName.setText(contact.getName());
            binding.tvContactNumber.setText(contact.getPhoneNumber());

            RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .fallback(R.drawable.ic_image)
                .circleCrop();

            Glide.with(binding.ivContactPhoto.getContext())
                .load(contact.getPhotoUri())
                .apply(requestOptions)
                .into(binding.ivContactPhoto);

            itemView.setOnClickListener(v -> listener.onContactClick(contact));
            binding.btnCallContact.setOnClickListener(v -> listener.onContactClick(contact));
            binding.btnEditContact.setOnClickListener(v -> listener.onContactEdit(contact));
        }
    }
}
