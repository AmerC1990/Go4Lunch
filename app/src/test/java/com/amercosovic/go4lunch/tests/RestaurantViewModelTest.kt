package com.amercosovic.go4lunch.tests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amercosovic.go4lunch.viewmodels.RestaurantsViewModel
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RestaurantViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun getNearbyPlaces_PostValueToLiveData() {
        // Given a fresh ViewModel
        val restaurantsViewModel = RestaurantsViewModel(ApplicationProvider.getApplicationContext())

        // When fetching nearby places
        restaurantsViewModel.fetchNearbyPlacesData("36.0726", "79.7920")

        // Then the new state value is posted
        val value = restaurantsViewModel.state

        MatcherAssert.assertThat(
            value.getOrAwaitValue(),
            CoreMatchers.not(CoreMatchers.nullValue())
        )
    }

    @Test
    fun getPlaceDetails_PostValueToLiveData() {
        // Given a fresh ViewModel
        val restaurantsViewModel = RestaurantsViewModel(ApplicationProvider.getApplicationContext())

        // When fetching place details
        restaurantsViewModel.fetchWebsiteAndPhoneNumberData("ChIJY31kehwbU4gRxuEGnqSlHyY")

        // Then the state2 value is posted
        val value = restaurantsViewModel.state2

        MatcherAssert.assertThat(
            value.getOrAwaitValue(),
            CoreMatchers.not(CoreMatchers.nullValue())
        )
    }
}