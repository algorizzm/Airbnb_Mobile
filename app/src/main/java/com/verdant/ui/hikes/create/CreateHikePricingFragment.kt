package com.verdant.ui.hikes.create

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.verdant.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.verdant.databinding.FragmentCreateHikePricingBinding
import kotlinx.coroutines.launch

class CreateHikePricingFragment : Fragment(R.layout.fragment_create_hike_pricing) {

    private var _binding: FragmentCreateHikePricingBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateHikePricingBinding.bind(view)

        binding.etPrice.doAfterTextChanged { flowVm.updatePriceText(it?.toString().orEmpty()) }
        binding.etPaymentMethods.doAfterTextChanged {
            flowVm.updatePaymentMethodsText(it?.toString().orEmpty())
        }
        binding.etPricingNotes.doAfterTextChanged {
            flowVm.updatePricingNotes(it?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flowVm.ui.collect { s ->
                    if (binding.etPrice.text?.toString() != s.priceText) {
                        binding.etPrice.setText(s.priceText)
                    }
                    if (binding.etPaymentMethods.text?.toString() != s.paymentMethodsText) {
                        binding.etPaymentMethods.setText(s.paymentMethodsText)
                    }
                    if (binding.etPricingNotes.text?.toString() != s.pricingNotes) {
                        binding.etPricingNotes.setText(s.pricingNotes)
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
