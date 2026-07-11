package s28.system.phone.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import s28.system.phone.databinding.ItemContactBinding;
import s28.system.phone.models.Contact;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
    private final List<Contact> contacts;
    private final OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    public ContactsAdapter(List<Contact> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
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
        holder.bind(contacts.get(position));
    }

    @Override
    public int getItemCount() { return contacts.size(); }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactBinding binding;

        public ContactViewHolder(ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Contact contact) {
            binding.tvContactName.setText(contact.getName());
            binding.tvContactNumber.setText(contact.getPhoneNumber());
            
            Glide.with(binding.ivContactPhoto.getContext())
                .load(contact.getPhotoUri())
                .placeholder(android.R.drawable.ic_menu_report_image)
                .circleCrop()
                .into(binding.ivContactPhoto);

            itemView.setOnClickListener(v -> listener.onContactClick(contact));
        }
    }
}
