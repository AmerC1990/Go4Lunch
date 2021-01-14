package com.amercosovic.go4lunch.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.model.Users
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import kotlinx.android.synthetic.main.single_restaurant_user_row.view.*

class AllUsersAdapter(
    options: FirestoreRecyclerOptions<Users>
) :
    FirestoreRecyclerAdapter<Users, AllUsersAdapter.UserAdapterVH>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAdapterVH {
        return UserAdapterVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.all_user_restaurant_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: UserAdapterVH, position: Int, model: Users) {
        holder.userName.text =
            model.userName.toString() + " is eating at " + model.userRestaurant.toString()

        Glide.with(holder.userProfilePic)
            .load(model.userImage.toString())
            .centerCrop()
            .circleCrop()
            .placeholder(R.drawable.default_colleague_icon)
            .error(R.drawable.default_colleague_icon)
            .into(holder.userProfilePic)
    }

    class UserAdapterVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var userName = itemView.usernameTextView
        var userProfilePic = itemView.userProfilePicImageView
    }
}