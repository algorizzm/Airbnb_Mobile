package com.airbnb.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.auth.AuthState
import com.airbnb.core.ui.AvatarHelper
import com.airbnb.core.ui.GuestPromptDialog
import com.airbnb.data.model.FeedItem
import com.airbnb.data.model.HikePost
import com.airbnb.data.remote.WeatherState
import com.airbnb.databinding.ItemHomeCtaBinding
import com.airbnb.databinding.ItemPostBinding
import com.airbnb.utils.Permissions

class PostAdapter(
    private val items: List<FeedItem>,
    private val userRole: String?,
    private val fragmentManager: FragmentManager,
    private val weather: WeatherState? = null,
    private val weatherLoading: Boolean = false,
    private val onHikeClick: () -> Unit,
    private val onPostClick: (HikePost) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CTA  = 0
        private const val TYPE_POST = 1
    }

    inner class CTAViewHolder(val binding: ItemHomeCtaBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HikeViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (items[position]) {
        is FeedItem.CTA  -> TYPE_CTA
        is FeedItem.Post -> TYPE_POST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_CTA -> CTAViewHolder(
                ItemHomeCtaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> HikeViewHolder(
                ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is FeedItem.CTA -> {
                val b = (holder as CTAViewHolder).binding
                b.btnHike.setOnClickListener { onHikeClick() }

                // Weather loading spinner
                b.progressWeather.visibility =
                    if (weatherLoading) View.VISIBLE else View.GONE

                // Weather chip
                if (weather != null) {
                    b.layoutWeather.visibility = View.VISIBLE
                    b.tvWeatherEmoji.text = weather.emoji
                    b.tvWeatherTemp.text = "${weather.tempC}°C"
                    b.tvWeatherCondition.text = weather.condition

                    // Hiking advisory badge
                    if (weather.isGoodForHiking) {
                        b.tvHikingAdvisory.visibility = View.VISIBLE
                        b.tvCtaSubtitle.text = "Perfect weather for a hike today!"
                    } else {
                        b.tvHikingAdvisory.visibility = View.GONE
                        b.tvCtaSubtitle.text = "Discover hikes near you"
                    }
                } else {
                    b.layoutWeather.visibility = View.GONE
                    b.tvHikingAdvisory.visibility = View.GONE
                    b.tvCtaSubtitle.text = "Discover hikes near you"
                }
            }
            is FeedItem.Post -> bindPost(holder as HikeViewHolder, item.hikePost)
        }
    }

    private fun bindPost(holder: HikeViewHolder, hike: HikePost) {
        with(holder.binding) {

            tvUsername.text = hike.username
            tvDate.text     = "${hike.date} at ${hike.time}"
            tvLoc.text      = "@${hike.location}"
            tvTitle.text    = hike.title
            tvStats.text    = "${hike.distance} • ${hike.elevation} • ${hike.duration}"

            AvatarHelper.bind(imgProfile, tvPostInitial, hike.username, null)

            rvImages.layoutManager = LinearLayoutManager(
                root.context, LinearLayoutManager.HORIZONTAL, false
            )
            rvImages.adapter = PostImageAdapter(hike.images)

            // Whole card click → post detail
            root.setOnClickListener { onPostClick(hike) }

            // RBAC
            val canLike    = Permissions.canLikePosts(userRole)
            val canComment = Permissions.canCommentPosts(userRole)
            val canShare   = Permissions.canSharePosts(userRole)

            btnLike.alpha    = if (canLike) 1f else 0.4f
            btnComment.alpha = if (canComment) 1f else 0.4f
            btnShare.alpha   = if (canShare) 1f else 0.4f

            // Guest guard helper — shows dialog if not authenticated
            fun guardedAction(action: () -> Unit) {
                if (AuthManager.stateSnapshot() is AuthState.Guest) {
                    GuestPromptDialog.show(fragmentManager)
                } else {
                    action()
                }
            }

            btnLike.setOnClickListener {
                guardedAction {
                    btnLike.setColorFilter(0xFF02D083.toInt())
                }
            }
            btnComment.setOnClickListener {
                guardedAction { /* open comment sheet */ }
            }
            btnShare.setOnClickListener {
                guardedAction { /* open share sheet */ }
            }
        }
    }

    override fun getItemCount() = items.size
}