package com.airbnb.ui.hikes.create

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.ui.explore.ExploreFragment

internal fun Fragment.createHikeFlowViewModel(): CreateHikeFlowViewModel {

    return ViewModelProvider(
        requireActivity(),
        CreateHikeFlowViewModel.Factory(
            requireActivity()
                .intent
                ?.getStringExtra(ExploreFragment.ARG_HIKE_ID)
        )
    )[CreateHikeFlowViewModel::class.java]
}
