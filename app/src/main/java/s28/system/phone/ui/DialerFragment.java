package s28.system.phone.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import s28.system.phone.databinding.FragmentDialerBinding;
import s28.system.phone.databinding.ItemDialpadButtonBinding;

public class DialerFragment extends Fragment {
    private FragmentDialerBinding binding;
    private StringBuilder phoneNumber = new StringBuilder();

    private static final String[] DIGITS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
    private static final String[] LETTERS = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", ""};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDialerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSettings.setOnClickListener(v -> {
            startActivity(new android.content.Intent(getContext(), s28.system.phone.SettingsActivity.class));
        });

        binding.dialpadGrid.setAdapter(new DialpadAdapter());
        
        binding.fabCall.setOnClickListener(v -> {
            String number = phoneNumber.toString();
            if (!number.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + number));
                startActivity(intent);
            }
        });

        binding.btnDelete.setOnClickListener(v -> {
            if (phoneNumber.length() > 0) {
                phoneNumber.deleteCharAt(phoneNumber.length() - 1);
                updatePhoneNumberDisplay();
            }
        });

        binding.btnDelete.setOnLongClickListener(v -> {
            if (phoneNumber.length() > 0) {
                phoneNumber.setLength(0);
                updatePhoneNumberDisplay();
            }
            return true;
        });

        binding.etPhoneNumber.setOnClickListener(null); // Remove old listener
    }

    private void updatePhoneNumberDisplay() {
        binding.etPhoneNumber.setText(phoneNumber.toString());
        binding.btnDelete.setVisibility(phoneNumber.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private class DialpadAdapter extends BaseAdapter {
        @Override
        public int getCount() { return DIGITS.length; }
        @Override
        public Object getItem(int position) { return DIGITS[position]; }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemDialpadButtonBinding itemBinding;
            if (convertView == null) {
                itemBinding = ItemDialpadButtonBinding.inflate(getLayoutInflater(), parent, false);
                convertView = itemBinding.getRoot();
                convertView.setTag(itemBinding);
            } else {
                itemBinding = (ItemDialpadButtonBinding) convertView.getTag();
            }

            itemBinding.tvDigit.setText(DIGITS[position]);
            itemBinding.tvLetters.setText(LETTERS[position]);

            convertView.setOnClickListener(v -> {
                phoneNumber.append(DIGITS[position]);
                updatePhoneNumberDisplay();
            });

            if (DIGITS[position].equals("0")) {
                convertView.setOnLongClickListener(v -> {
                    phoneNumber.append("+");
                    updatePhoneNumberDisplay();
                    return true;
                });
            }

            return convertView;
        }
    }
}
