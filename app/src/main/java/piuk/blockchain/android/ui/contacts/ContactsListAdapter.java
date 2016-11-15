package piuk.blockchain.android.ui.contacts;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import piuk.blockchain.android.R;

class ContactsListAdapter extends RecyclerView.Adapter<ContactsListAdapter.ContactsViewHolder> {

    private List<ContactsListItem> contacts;
    private ContactsClickListener contactsClickListener;

    ContactsListAdapter(List<ContactsListItem> contacts) {
        this.contacts = contacts;
    }

    void setContactsClickListener(ContactsClickListener contactsClickListener) {
        this.contactsClickListener = contactsClickListener;
    }

    @Override
    public ContactsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactsViewHolder holder, int position) {
        ContactsListItem listItem = contacts.get(position);

        holder.name.setText(listItem.getContactName());
        holder.status.setText(listItem.getStatus());
        holder.itemView.setOnClickListener(view -> {
            if (contactsClickListener != null) contactsClickListener.onClick(listItem.getMdid());
        });
    }

    @Override
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }

    static class ContactsViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView status;

        ContactsViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.contact_name);
            status = (TextView) itemView.findViewById(R.id.contact_status);
        }
    }

    interface ContactsClickListener {

        void onClick(String mdid);

    }
}