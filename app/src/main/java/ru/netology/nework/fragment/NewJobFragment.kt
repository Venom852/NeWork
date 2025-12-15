package ru.netology.nework.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.netology.nework.R
import ru.netology.nework.databinding.CardCalendarBinding
import ru.netology.nework.databinding.FragmentNewJobBinding
import ru.netology.nework.databinding.SelectDateJobBinding
import java.util.Calendar

class NewJobFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentNewJobBinding.inflate(layoutInflater, container, false)
        val bindingSelectDateJob =
            SelectDateJobBinding.inflate(layoutInflater, container, false)
        val bindingCardCalendar =
            CardCalendarBinding.inflate(layoutInflater, container, false)

        val dialog = BottomSheetDialog(requireContext())
        val dialogCalendar = BottomSheetDialog(requireContext())
        var conditionCalendar = 0
        //TODO(Добавить viewModel)

        applyInset(binding.root)

        with(binding) {
            back.setOnClickListener {
                findNavController().navigateUp()
            }

            dateInput.setOnClickListener {
                dialog.setCancelable(false)
                dialog.setContentView(bindingSelectDateJob.root)
                dialog.show()
            }

            create.setOnClickListener {
                if (titleJobInput.text.toString().isEmpty()) {
                    titleJobInput.error = getString(R.string.empty_login)
                    titleJobInput.error = null
                }

                if (jobPostInput.text.toString().isEmpty()) {
                    jobPostInput.error = getString(R.string.empty_password)
                    jobPostInput.error = null
                }

                if (dateInput.text.toString().isEmpty()) {
                    dateInput.error = getString(R.string.empty_password)
                    dateInput.error = null
                }

                if (!titleJobInput.text.toString().isEmpty() &&
                    !jobPostInput.text.toString().isEmpty() &&
                    !dateInput.text.toString().isEmpty()
                ) {
                    viewModel.signIn(titleJobInput.text.toString(), jobPostInput.text.toString())
                }
            }
        }

        with(bindingSelectDateJob) {
            calendarButton.setOnClickListener {
                dialogCalendar.setCancelable(false)
                dialogCalendar.setContentView(bindingCardCalendar.root)
                dialogCalendar.show()
            }

            cancel.setOnClickListener {
                dialog.dismiss()
            }

            ok.setOnClickListener {
                //TODO(Подумать как правильно конвертировать дату)
                if (!startDateInput.text.toString().isEmpty() && !endDateInput.text.toString().isEmpty()) {
                    val text = "$startDateInput - $endDateInput"
                    binding.dateInput.setText(text)
                    //TODO(Добавить viewModel)
                } else {
                    val text = "$startDateInput - ${context?.getString(R.string.present_time)}"
                    binding.dateInput.setText(text)
                    //TODO(Добавить viewModel)
                }
            }
        }

        bindingCardCalendar.calendarView.setOnDateChangeListener { view, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)

            if (conditionCalendar == 0) {
                conditionCalendar = 1
                val date = "$month/$day/$year"
                bindingSelectDateJob.startDateInput.setText(date)
            } else {
                conditionCalendar = 1
                val date = "$month/$day/$year"
                bindingSelectDateJob.endDateInput.setText(date)
                dialogCalendar.dismiss()
            }
        }

        return binding.root
    }

    private fun applyInset(main: View) {
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            v.setPadding(
                v.paddingLeft,
                systemBars.top,
                v.paddingRight,
                if (isImeVisible) imeInsets.bottom else systemBars.bottom
            )
            insets
        }
    }
}