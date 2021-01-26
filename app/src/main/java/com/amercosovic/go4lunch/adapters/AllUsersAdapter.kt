package com.amercosovic.go4lunch.adapters

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.activities.RestaurantDetailsActivity
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

        if (model.userRestaurant.toString().contains("undecided")) {
            holder.userName.text = model.userName.toString() + " hasn't decided yet"
            holder.userName.textSize = 16F
            val myCustomFont: Typeface? =
                ResourcesCompat.getFont(holder.itemView.context, R.font.catamaran_bold)
            holder.userName.setTypeface(myCustomFont, Typeface.ITALIC)
            holder.userName.setTextColor(Color.parseColor("#AFACAC"))

            Glide.with(holder.userProfilePic)
                .load(model.userImage.toString())
                .centerCrop()
                .circleCrop()
                .placeholder(R.drawable.default_colleague_icon)
                .error(R.drawable.default_colleague_icon)
                .into(holder.userProfilePic)
        } else if (!model.userRestaurant.toString().contains("undecided")) {
            holder.userName.text = model.userName.toString()
                .substringBefore(" ") + " is eating at " + model.userRestaurant.toString()

            Glide.with(holder.userProfilePic)
                .load(model.userImage.toString())
                .centerCrop()
                .circleCrop()
                .placeholder(R.drawable.default_colleague_icon)
                .error(R.drawable.default_colleague_icon)
                .into(holder.userProfilePic)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RestaurantDetailsActivity::class.java)
            if (model.userRestaurant.toString().contains("undecided")) {
                Toast.makeText(
                    holder.itemView.context,
                    "${model.userName} is undecided on a restaurant",
                    Toast.LENGTH_LONG
                ).show()
            } else if (model.userRestaurantData?.isNullOrEmpty() == false) {
                intent.putExtra("userRestaurantData", model.userRestaurantData)
                Log.d("userRestaurantData", model.userRestaurantData)
                holder.itemView.context.startActivity(intent)
            }

        }
    }

    class UserAdapterVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var userName = itemView.usernameTextView
        var userProfilePic = itemView.userProfilePicImageView
    }
}