package com.amercosovic.go4lunch.tests

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.amercosovic.go4lunch.viewmodels.RestaurantsViewModel
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RestaurantViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    @Throws(Exception::class)
    fun getNearbyPlaces_PostValueToLiveData() {
        // Given a fresh ViewModel
        val application = RuntimeEnvironment.application
        val restaurantsViewModel = RestaurantsViewModel(application)

        // When fetching nearby places
        restaurantsViewModel.fetchNearbyPlacesData("36.0726", "79.7920")

        // Then the nearbyPlacesState value is posted
        val value = restaurantsViewModel.nearbyPlacesState

        // assert that it is not null
        MatcherAssert.assertThat(
            value.getOrAwaitValue(),
            CoreMatchers.not(CoreMatchers.nullValue())
        )
    }

    @Test
    @Throws(Exception::class)
    fun getPlaceDetails_PostValueToLiveData() {
        // Given a fresh ViewModel
        val application = RuntimeEnvironment.application
        val restaurantsViewModel = RestaurantsViewModel(application)

        // When fetching place details
        restaurantsViewModel.fetchWebsiteAndPhoneNumberData("ChIJY31kehwbU4gRxuEGnqSlHyY")

        // Then the placeDetailsState value is posted
        val value = restaurantsViewModel.placeDetailsState

        // assert that it is not null
        MatcherAssert.assertThat(
            value.getOrAwaitValue(),
            CoreMatchers.not(CoreMatchers.nullValue())
        )
    }
}