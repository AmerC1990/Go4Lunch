package com.amercosovic.go4lunch

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.amercosovic.go4lunch.activities.LoginActivity
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginActivityUITest {
    /* Instantiate an ActivityScenarioRule object. */
    @get:Rule
    var loginRule: ActivityScenarioRule<LoginActivity> =
        ActivityScenarioRule(LoginActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    // test that username edit text becomes visible when register is selected
    @Test
    fun userNameEditTestDisplaysWhenCreateAccountIsClicked() {
        // check that username edit text is invisible
        Espresso.onView(ViewMatchers.withId(R.id.editTextUsername))
            .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
        // click on "Create Account" / register button
        Espresso.onView(ViewMatchers.withId(R.id.registerButton)).perform(ViewActions.click())
        // check that username edit text becomes visible
        Espresso.onView(ViewMatchers.withId(R.id.editTextUsername))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    // test that google login open map fragment
    @Test
    fun googleLoginOpensMapFragment() {
        // login from Login Activity
        Espresso.onView(ViewMatchers.withId(R.id.googleLoginButton)).perform(ViewActions.click())
        // check that Map Fragment is displayed
        Espresso.onView(ViewMatchers.withId(R.id.mapFragment))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    // test that facebook login opens map fragment
    @Test
    fun facebookLoginOpensMapFragment() {
        // login from Login Activity
        Espresso.onView(ViewMatchers.withId(R.id.facebookLoginButton)).perform(ViewActions.click())
        // check that Map Fragment is displayed
        Espresso.onView(ViewMatchers.withId(R.id.mapFragment))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    // test that email and passwork fields enter matching data when entered in them
    @Test
    fun checkEditTextInputs() {
        // enter text in email edit text and check that it matches
        Espresso.onView(ViewMatchers.withId(R.id.editTextEmailAddress)).perform(
            ViewActions.typeText("sampleemail@email.com")
        )
            .check(ViewAssertions.matches(ViewMatchers.withText("sampleemail@email.com")))

        // enter text in password edit text and check that it matches
        Espresso.onView(ViewMatchers.withId(R.id.editTextPassword)).perform(
            ViewActions.typeText("samplepassword")
        )
            .check(ViewAssertions.matches(ViewMatchers.withText("samplepassword")))
    }

}