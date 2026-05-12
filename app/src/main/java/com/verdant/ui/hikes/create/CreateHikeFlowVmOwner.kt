package com.verdant.ui.hikes.create

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.verdant.ui.explore.ExploreFragment

internal fun Fragment.createHikeFlowViewModel(): CreateHikeFlowViewModel {
    val host = requireParentFragment()
    return ViewModelProvider(
        host,
        CreateHikeFlowViewModel.Factory(
            host.requireArguments().getString(ExploreFragment.ARG_HIKE_ID)
        )
    )[CreateHikeFlowViewModel::class.java]
}
