package com.amercosovic.go4lunch.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.drawerlayout.widget.DrawerLayout
import com.amercosovic.go4lunch.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*


class YourLunchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layoutInflater = inflater.inflate(R.layout.fragment_yourlunch, container, false)
        val navDrawer: DrawerLayout = activity!!.drawer_layout
        navDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        val bottomNavBar: BottomNavigationView = activity!!.bottom_navigation
        bottomNavBar.visibility = View.VISIBLE
        val flWrapper: FrameLayout = activity!!.fl_wrapper
        flWrapper.layoutParams = LinearLayout.LayoutParams(720,1225)
        val actionBar: android.app.ActionBar? = activity!!.actionBar
        actionBar?.hide()
        return layoutInflater
    }
}