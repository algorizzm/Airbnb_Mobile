package com.verdant.core.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

/**
 * Lightweight avatar placeholder helper.
 *
 * Usage:
 *   AvatarHelper.bind(imgView, tvInitial, name = user.name, imageUrl = user.profileImage)
 *
 * - If imageUrl is non-blank → loads via Glide, hides tvInitial
 * - Otherwise → shows first uppercase letter of name on a colored circle
 * - Color is deterministic per name (hash-based), so it never changes between renders
 */
object AvatarHelper {

    // 10 distinct dark-friendly accent colors
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
     * Bind an avatar slot.
     *
     * @param imgView   The circular ImageView (used for real photos)
     * @param tvInitial The TextView overlaid on imgView (used for initials)
     * @param name      Display name or username — first char becomes the initial
     * @param imageUrl  Firebase Storage / remote URL; empty/null triggers initials
     */
    fun bind(
        imgView: ImageView,
        tvInitial: TextView,
        name: String?,
        imageUrl: String? = null
    ) {
        if (!imageUrl.isNullOrBlank()) {
            // Real photo — hide initials, load image
            tvInitial.visibility = android.view.View.GONE
            Glide.with(imgView.context)
                .load(imageUrl)
                .circleCrop()
                .placeholder(circleDrawable(colorFor(name)))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgView)
        } else {
            // No photo — show colored circle + initial letter
            val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val color = colorFor(name)

            imgView.setImageDrawable(circleDrawable(color))
            tvInitial.text = initial
            tvInitial.visibility = android.view.View.VISIBLE
        }
    }

    /** Deterministic color from name/uid — same input always returns same color. */
    fun colorFor(key: String?): Int {
        val index = Math.abs((key ?: "").hashCode()) % COLORS.size
        return Color.parseColor(COLORS[index])
    }

    /** Solid circle GradientDrawable for use as ImageView src or background. */
    fun circleDrawable(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
}
