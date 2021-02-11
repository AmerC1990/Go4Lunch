package com.amercosovic.go4lunch.adapters

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
import java.util.*

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
        // bind data
        if (model.userRestaurant.toString().contains("undecided")) {
            holder.userName.text = model.userName.toString() + translate(
                english = " hasn't decided yet",
                spanish = " no ha decidido todavía"
            )
            holder.userName.textSize = 16F
            val myCustomFont: Typeface? =
                ResourcesCompat.getFont(holder.itemView.context, R.font.catamaran_bold)
            holder.userName.setTypeface(myCustomFont, Typeface.ITALIC)
            holder.userName.setTextColor(Color.parseColor("#AFACAC"))

            Glide.with(holder.userProfilePic)
                .load(model.userImage.toString())
                .centerCrop()
                .circleCrop()
                .placeholder(R.drawable.defaultprofilepicture)
                .error(R.drawable.defaultprofilepicture)
                .into(holder.userProfilePic)
        } else if (!model.userRestaurant.toString().contains("undecided")) {
            holder.userName.text = model.userName.toString()
                .substringBefore(" ") + translate(
                english = " is eating at ",
                spanish = " está comiendo en "
            ) + model.userRestaurant.toString()
            holder.userName.textSize = 16F
            val myCustomFont: Typeface? =
                ResourcesCompat.getFont(holder.itemView.context, R.font.catamaran_bold)
            holder.userName.setTypeface(myCustomFont, Typeface.BOLD)
            holder.userName.setTextColor(Color.parseColor("#070606"))

            Glide.with(holder.userProfilePic)
                .load(model.userImage.toString())
                .centerCrop()
                .circleCrop()
                .placeholder(R.drawable.defaultprofilepicture)
                .error(R.drawable.defaultprofilepicture)
                .into(holder.userProfilePic)
        }
        // set up on click listener and open new activity while passing data to it for particular restaurant
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RestaurantDetailsActivity::class.java)
            if (model.userRestaurant.toString().contains("undecided")) {
                Toast.makeText(
                    holder.itemView.context,
                    "${model.userName} ${
                        translate(
                            english = "hasn't decided on a restaurant yet!",
                            spanish = "Está indecisa en un restaurante"
                        )
                    }",
                    Toast.LENGTH_LONG
                ).show()
            } else if (model.userRestaurantData?.isNullOrEmpty() == false) {
                intent.putExtra("userRestaurantData", model.userRestaurantData)
                holder.itemView.context.startActivity(intent)
            }

        }
    }

    class UserAdapterVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var userName = itemView.usernameTextView
        var userProfilePic = itemView.userProfilePicImageView
    }

    // translate
    private fun translate(spanish: String, english: String): String {
        val language = Locale.getDefault().displayLanguage

        return if (language.toString() == "español") {
            return spanish
        } else {
            return english
        }
    }
}