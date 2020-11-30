package com.amercosovic.go4lunch.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import com.amercosovic.go4lunch.R
import kotlinx.android.synthetic.main.activity_main.*


class YourLunchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layoutInflater = inflater.inflate(R.layout.fragment_yourlunch, container, false)
        val navDrawer: DrawerLayout = activity!!.mainActivityDrawerLayout
        navDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        return layoutInflater
    }
}