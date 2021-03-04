package com.amercosovic.go4lunch

import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Checkable
import android.widget.SearchView
import androidx.test.espresso.*
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.amercosovic.go4lunch.activities.LoginActivity
import com.amercosovic.go4lunch.activities.MainActivity
import com.amercosovic.go4lunch.activities.RestaurantDetailsActivity
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityUITest {
    /* Instantiate an ActivityScenarioRule object. */
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    // test nav drawer: your lunch click
    @Test
    fun yourLunchNavDrawerClick() {
        // check if nav drawer is closed, if so, open it
        Espresso.onView(withId(R.id.mainActivityDrawerLayout))
            .check(matches(isClosed(Gravity.LEFT))).perform(DrawerActions.open())
        // check that nav drawer is open
        Espresso.onView(withId(R.id.mainActivityDrawerLayout)).check(matches(isOpen(Gravity.LEFT)))
        // click on "Your Lunch"
        Espresso.onView(ViewMatchers.withId(R.id.yourLunchButton)).perform(ViewActions.click())
        // Check if Toast opens, if not , check if Restaurant Details Activity opens
        try {
            Espresso.onView(
                anyOf(
                    ViewMatchers.withText("You haven't decided on a restaurant yet!"),
                    ViewMatchers.withId(R.id.imageOfRestaurant),
                    ViewMatchers.withText("You haven't decided on a restaurant yet!")
                )
            ).inRoot(ToastMatcher()).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        } catch (e: NoMatchingRootException) {
//            Espresso.onView(anyOf(ViewMatchers.withText("You haven't decided on a restaurant yet!"),ViewMatchers.withId(R.id.imageOfRestaurant))).inRoot(ToastMatcher()).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Intents.intended(hasComponent(RestaurantDetailsActivity::class.java.name))
        }
    }

    // test nav drawer: settings click
    @Test
    fun settingsNavDrawerClick() {
        // check if nav drawer is closed, if so, open it
        Espresso.onView(withId(R.id.mainActivityDrawerLayout))
            .check(matches(isClosed(Gravity.LEFT))).perform(DrawerActions.open())
        // check that nav drawer is open
        Espresso.onView(withId(R.id.mainActivityDrawerLayout)).check(matches(isOpen(Gravity.LEFT)))
        // click on "Settings"
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton)).perform(ViewActions.click())
        // check if Settings Fragment opens
        Espresso.onView(withId(R.id.settingsFragment))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // check that toggle/switch turns on/off when clicked and make sure state matches
        setChecked(true)
    }

    // test nav drawer: logout click
    @Test
    fun logoutNavDrawerClick() {
        // check if nav drawer is closed, if so, open it
        Espresso.onView(withId(R.id.mainActivityDrawerLayout))
            .check(matches(isClosed(Gravity.LEFT))).perform(DrawerActions.open())
        // check that nav drawer is open
        Espresso.onView(withId(R.id.mainActivityDrawerLayout)).check(matches(isOpen(Gravity.LEFT)))
        // click on "Logout"
        Espresso.onView(ViewMatchers.withId(R.id.logoutButton)).perform(ViewActions.click())
        // check if Login Activity opens
        Intents.intended(hasComponent(LoginActivity::class.java.name))
    }

    // test nav drawer: open/close functionality
    @Test
    fun navDrawerOpenCloseFunctionality() {
        // check that nav drawer is closed, open it
        Espresso.onView(withId(R.id.mainActivityDrawerLayout))
            .check(matches(isClosed(Gravity.LEFT))).perform(DrawerActions.open())
        // check that nav drawer is open, close it
        Espresso.onView(withId(R.id.mainActivityDrawerLayout)).check(matches(isOpen(Gravity.LEFT)))
            .perform(DrawerActions.close())
        // check that nav drawer is closed again
        Espresso.onView(withId(R.id.mainActivityDrawerLayout))
            .check(matches(isClosed(Gravity.LEFT)))
    }

    // test search view taking proper text
    @Test
    fun doesSearchTakeTextProperly() {
        // type text in search view and check that it matches
        typeSearchViewText("test")
    }

    // test nav bar clicks
    @Test
    fun navBarClicks() {
        // click on list view nav bar item
        Espresso.onView(ViewMatchers.withId(R.id.bottomNavigation)).perform(ViewActions.click())
//        // check that restaurant list fragment is open
        Espresso.onView(withId(R.id.restaurantListFragment))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}

// Toast matcher class to get type safe matcher for toast
class ToastMatcher : TypeSafeMatcher<Root>() {
    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    override fun matchesSafely(root: Root): Boolean {
        val type = root.windowLayoutParams.get().type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken = root.decorView.windowToken
            val appToken = root.decorView.applicationWindowToken
            if (windowToken === appToken) {
                return true
            }
        }
        return false
    }
}

// fun to enter text in search view
fun typeSearchViewText(text: String): ViewAction {
    return object : ViewAction {
        override fun getDescription(): String {
            return "Change view text"
        }

        override fun getConstraints(): Matcher<View> {
            return allOf(isDisplayed(), isAssignableFrom(SearchView::class.java))
        }

        override fun perform(uiController: UiController?, view: View?) {
            (view as SearchView).setQuery(text, false)
        }
    }
}

// function to check state of switch/toggle button regardless of whether it is on or off when clicked
fun setChecked(checked: Boolean) = object : ViewAction {
    val checkableViewMatcher = object : BaseMatcher<View>() {
        override fun matches(item: Any?): Boolean = isA(Checkable::class.java).matches(item)
        override fun describeTo(description: Description?) {
            description?.appendText("is Checkable instance ")
        }
    }

    override fun getConstraints(): BaseMatcher<View> = checkableViewMatcher
    override fun getDescription(): String? = null
    override fun perform(uiController: UiController?, view: View) {
        val checkableView: Checkable = view as Checkable
        checkableView.isChecked = checked
    }
}