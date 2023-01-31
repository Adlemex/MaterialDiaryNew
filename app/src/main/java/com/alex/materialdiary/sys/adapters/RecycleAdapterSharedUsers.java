package com.alex.materialdiary.sys.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.alex.materialdiary.ChangeUserFragment;
import com.alex.materialdiary.NewChangeUserFragment;
import com.alex.materialdiary.R;
import com.alex.materialdiary.ShareQRFragmentDirections;
import com.alex.materialdiary.WebLoginFragmentDirections;
import com.alex.materialdiary.sys.common.CommonAPI;
import com.alex.materialdiary.sys.common.models.ShareUser;
import com.alex.materialdiary.sys.common.models.get_user.Participant;
import com.alex.materialdiary.sys.common.models.get_user.SchoolInfo;
import com.alex.materialdiary.sys.common.models.get_user.Schools;

import java.util.ArrayList;
import java.util.List;

public class RecycleAdapterSharedUsers extends RecyclerView.Adapter<RecycleAdapterSharedUsers.ViewHolder>{

    private final LayoutInflater inflater;
    private final NewChangeUserFragment fragment;
    private final List<ShareUser> users;
    public RecycleAdapterSharedUsers(NewChangeUserFragment context, List<ShareUser> users) {
        this.inflater = LayoutInflater.from(context.requireContext());
        this.users = users;
        fragment = context;
    }

    @NonNull
    @Override
    public RecycleAdapterSharedUsers.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.users_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecycleAdapterSharedUsers.ViewHolder holder, int position) {
        ShareUser user = users.get(position);
        holder.Name.setText(user.getName());
        holder.Grade.setText(user.getClassname() + " класс");
        holder.SchoolName.setText(user.getSchool());
        /*holder.scan.setOnClickListener(v -> {
            NavDirections action =
                    WebLoginFragmentDirections.toWebLogin(user.getGuid(),
                            user.getName());
            Navigation.findNavController(fragment.requireActivity(), R.id.nav_host_fragment_content_main).navigate(action);
        });*/
        holder.share.setOnClickListener(v -> {
            NavDirections action =
                    ShareQRFragmentDirections.toShare(user);
            Navigation.findNavController(fragment.requireActivity(), R.id.nav_host_fragment_content_main).navigate(action);
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonAPI.getInstance().ChangeUuid(user.getGuid());
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView Grade;
        TextView Name;
        TextView SchoolName;
        ImageView scan;
        ImageView share;
        // Get the handles by calling findViewById() on View object inside the constructor
        ViewHolder(View v)
        {
            super(v);
            Grade = v.findViewById(R.id.user_Grade);
            Name = v.findViewById(R.id.user_Name);
            SchoolName = v.findViewById(R.id.user_SchoolName);
            scan = v.findViewById(R.id.scanQr);
            share = v.findViewById(R.id.shareQr);
        }
    }
}
