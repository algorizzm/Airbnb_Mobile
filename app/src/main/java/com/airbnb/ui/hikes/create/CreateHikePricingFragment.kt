package com.airbnb.ui.hikes.create

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.R
import com.airbnb.databinding.FragmentCreateHikePricingBinding
import kotlinx.coroutines.launch

class CreateHikePricingFragment :
    Fragment(R.layout.fragment_create_hike_pricing) {

    private var _binding: FragmentCreateHikePricingBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        _binding =
            FragmentCreateHikePricingBinding.bind(view)

        setupPaymentDropdown()

        setupInputs()

        observeUi()
    }

    private fun setupPaymentDropdown() {

        val paymentMethods = listOf(
            "Cash",
            "GCash",
            "Maya",
            "Bank Transfer",
            "BDO",
            "BPI",
            "Metrobank",
            "UnionBank",
            "GoTyme Bank",
            "Cebuana Lhuillier",
            "Palawan Express",
            "Coins.ph",
            "7-Eleven CLIQQ",
            "Cash Meetup",
            "Cash on Meetup"
        )

        val paymentAdapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                paymentMethods
            )

        binding.etPaymentMethods.setAdapter(
            paymentAdapter
        )

        // force default value
        if (
            binding.etPaymentMethods.text.isNullOrBlank()
        ) {

            binding.etPaymentMethods.setText(
                "Cash",
                false
            )
        }
    }

    private fun setupInputs() {

        binding.etPrice.doAfterTextChanged {

            flowVm.updatePriceText(
                it?.toString().orEmpty()
            )
        }

        binding.etPaymentMethods.doAfterTextChanged {

            flowVm.updatePaymentMethodsText(
                it?.toString().orEmpty()
            )
        }

        binding.etPricingNotes.doAfterTextChanged {

            flowVm.updatePricingNotes(
                it?.toString().orEmpty()
            )
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                flowVm.ui.collect { s ->

                    if (
                        binding.etPrice.text?.toString()
                        != s.priceText
                    ) {

                        binding.etPrice.setText(
                            s.priceText
                        )
                    }

                    if (
                        binding.etPaymentMethods.text?.toString()
                        != s.paymentMethodsText
                    ) {

                        binding.etPaymentMethods.setText(
                            s.paymentMethodsText,
                            false
                        )
                    }

                    if (
                        binding.etPricingNotes.text?.toString()
                        != s.pricingNotes
                    ) {

                        binding.etPricingNotes.setText(
                            s.pricingNotes
                        )
                    }

                    binding.layoutFloatingCard.tvFloatingHikeTitle.text =
                        s.title.ifBlank {
                            "Untitled Hike"
                        }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}