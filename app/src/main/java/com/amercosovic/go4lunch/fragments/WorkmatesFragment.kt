package com.amercosovic.go4lunch.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.adapters.AllUsersAdapter
import com.amercosovic.go4lunch.model.Users
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_workmates.*

class WorkmatesFragment : Fragment() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collectionReference: CollectionReference = db.collection("users")
    var adapter: AllUsersAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_workmates, container, false)
    }

    override fun onStart() {
        super.onStart()
        setUpRecyclerView(collectionReference)
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    private fun setUpRecyclerView(collectionReference: CollectionReference) {
        val query: Query = collectionReference

        val firestoreRecyclerOptions: FirestoreRecyclerOptions<Users> =
            FirestoreRecyclerOptions.Builder<Users>()
                .setQuery(query, Users::class.java)
                .build()

        val decorator = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        adapter = AllUsersAdapter(firestoreRecyclerOptions)
        workmatesFragmentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        workmatesFragmentRecyclerView.adapter = adapter
        workmatesFragmentRecyclerView.addItemDecoration(decorator)
        adapter?.notifyDataSetChanged()
    }
}

