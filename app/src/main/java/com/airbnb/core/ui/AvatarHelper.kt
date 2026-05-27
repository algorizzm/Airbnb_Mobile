package com.airbnb.core.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

/**
 * Lightweight avatar placeholder helper.
 *
 * Usage:
 *   AvatarHelper.bind(
 *       imgView,
 *       tvInitial,
 *       name = user.name,
 *       imageUrl = user.profileImage
 *   )
 *
 * - If imageUrl exists → loads real image
 * - Otherwise → generates initials avatar
 * - Works with nullable tvInitial for bottom nav avatars
 */
object AvatarHelper {

    // Dark-theme friendly colors
    private val COLORS = listOf(
        "#E53935", // red
        "#8E24AA", // purple
        "#1E88E5", // blue
        "#00897B", // teal
        "#43A047", // green
        "#FB8C00", // orange
        "#6D4C41", // brown
        "#039BE5", // light blue
        "#D81B60", // pink
        "#546E7A"  // blue-grey
    )

    /**
     * Bind avatar image or initials placeholder.
     *
     * @param imgView   Image target
     * @param tvInitial Optional initials TextView
     * @param name      User display name
     * @param imageUrl  Optional avatar URL
     */
    fun bind(
        imgView: ImageView,
        tvInitial: TextView?,
        name: String?,
        imageUrl: String? = null
    ) {

        if (!imageUrl.isNullOrBlank()) {

            // Hide initials if image exists
            tvInitial?.visibility = View.GONE

            Glide.with(imgView.context)
                .load(imageUrl)
                .circleCrop()
                .placeholder(circleDrawable(colorFor(name)))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgView)

        } else {

            // Generate initials avatar
            val initial =
                name?.trim()?.firstOrNull()?.uppercaseChar()?.toString()
                    ?: "?"

            val color = colorFor(name)

            imgView.setImageDrawable(circleDrawable(color))

            tvInitial?.text = initial
            tvInitial?.visibility = View.VISIBLE
        }
    }

    /**
     * Deterministic avatar color.
     */
    fun colorFor(key: String?): Int {
        val index = Math.abs((key ?: "").hashCode()) % COLORS.size
        return Color.parseColor(COLORS[index])
    }

    /**
     * Circular drawable background.
     */
    fun circleDrawable(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
}