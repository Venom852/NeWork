package ru.netology.nework.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.CardCalendarBinding
import ru.netology.nework.databinding.FragmentNewJobBinding
import ru.netology.nework.databinding.SelectDateJobBinding
import ru.netology.nework.viewmodel.JobMyViewModel
import java.util.Calendar
import kotlin.getValue

@AndroidEntryPoint
class NewJobFragment : Fragment() {
    companion object {
        const val NEW_JOB = "newJob"
    }

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

        val viewModelJobMy: JobMyViewModel by activityViewModels()

        val dialog = BottomSheetDialog(requireContext())
        val dialogCalendar = BottomSheetDialog(requireContext())
        var conditionCalendar = 0
        var date = ""
        var dateStart = ""
        var dateEnd = ""

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
                    viewModelJobMy.saveJob(titleJobInput.text.toString(), jobPostInput.text.toString())
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
                when {
                    !startDateInput.text.toString().isEmpty() && !endDateInput.text.toString().isEmpty() -> {
                        val text = "$startDateInput - $endDateInput"
                        binding.dateInput.setText(text)

                        dateStart = "${dateStart}T00:00:00.000Z"
                        dateEnd = "${dateEnd}T00:00:00.000Z"

                        viewModelJobMy.saveDate(dateStart, dateEnd)
                        dialog.dismiss()
                    }

                    !startDateInput.text.toString().isEmpty() -> {
                        val text = "$startDateInput - ${context?.getString(R.string.present_time)}"
                        binding.dateInput.setText(text)

                        dateStart = "${dateStart}T00:00:00.000Z"

                        viewModelJobMy.saveDate(dateStart)
                        dialog.dismiss()
                    }

                    else -> {
                        dialog.dismiss()
                    }
                }
            }
        }

        bindingCardCalendar.calendarView.setOnDateChangeListener { view, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)

            if (conditionCalendar == 0) {
                conditionCalendar = 1
                date = "$day.$month.$year"
                dateStart = "$year-$month-$day"

                bindingSelectDateJob.startDateInput.setText(date)
            } else {
                conditionCalendar = 0
                date = "$day.$month.$year"
                dateEnd = "$year-$month-$day"

                bindingSelectDateJob.endDateInput.setText(date)
                dialogCalendar.dismiss()
            }
        }

        viewModelJobMy.jobCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        return binding.root
    }
}