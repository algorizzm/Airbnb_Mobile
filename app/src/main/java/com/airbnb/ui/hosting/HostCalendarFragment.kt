package com.airbnb.ui.hosting

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.airbnb.R

/**
 * Host Calendar tab — placeholder fragment for Sprint 8.
 *
 * Displays a "Calendar coming soon" empty state.
 * Full calendar implementation (reservation date overview,
 * availability blocking) is deferred to a future sprint.
 */
class HostCalendarFragment : Fragment(R.layout.fragment_host_calendar) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // No setup required for placeholder state
    }
}
