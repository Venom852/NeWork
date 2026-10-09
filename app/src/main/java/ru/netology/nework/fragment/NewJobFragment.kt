package ru.netology.nework.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
@SuppressLint("SetTextI18n")
class NewJobFragment : Fragment() {
    companion object {
        const val NEW_JOB = "newJob"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewJobBinding.inflate(layoutInflater, container, false)
        val bindingSelectDateJob =
            SelectDateJobBinding.inflate(layoutInflater, container, false)
        val bindingCardCalendar =
            CardCalendarBinding.inflate(layoutInflater, container, false)

        val viewModelJobMy: JobMyViewModel by activityViewModels()

        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogCalendar = BottomSheetDialog(requireContext())
        var conditionCalendar = 0
        var date: String
        var dateStart = ""
        var dateEnd = ""

        with(binding) {
            back.setOnClickListener {
                findNavController().navigateUp()
            }

            dateInput.setOnClickListener {
                dialog.setCancelable(false)
                dialog.setView(bindingSelectDateJob.root)
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.show()
            }

            create.setOnClickListener {
                if (titleJobInput.text.toString().isEmpty()) {
                    titleJobInput.error = getString(R.string.empty_login)
                }

                if (jobPostInput.text.toString().isEmpty()) {
                    jobPostInput.error = getString(R.string.empty_password)
                }

                if (dateInput.text.toString().isEmpty()) {
                    dateInput.error = getString(R.string.empty_password)
                }

                if (!titleJobInput.text.toString().isEmpty() &&
                    !jobPostInput.text.toString().isEmpty() &&
                    !dateInput.text.toString().isEmpty()
                ) {
                    if (!linkInput.text.toString().isEmpty()) {
                        viewModelJobMy.saveJob(titleJobInput.text.toString(), jobPostInput.text.toString(), linkInput.text.toString())
                    } else {
                        viewModelJobMy.saveJob(titleJobInput.text.toString(), jobPostInput.text.toString())
                    }
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
                        binding.dateInput.text = "${startDateInput.text.toString()} - ${endDateInput.text.toString()}"

                        dateStart = "${dateStart}T00:00:00.000Z"
                        dateEnd = "${dateEnd}T00:00:00.000Z"

                        viewModelJobMy.saveDate(dateStart, dateEnd)
                        dialog.dismiss()
                    }

                    !startDateInput.text.toString().isEmpty() -> {
                        binding.dateInput.text = "${startDateInput.text.toString()} - ${context?.getString(R.string.present_time)}"

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

        bindingCardCalendar.calendarView.setOnDateChangeListener { _, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)

            var monthString = month.toString()
            var dayString = day.toString()

            if (conditionCalendar == 0) {
                conditionCalendar = 1
                date = "$month/$day/$year"

                when {
                    monthString.length != 2 && dayString.length != 2 -> {
                        monthString = "0$monthString"
                        dayString = "0$dayString"
                        dateStart = "$year-$monthString-$dayString"
                    }

                    monthString.length != 2 -> {
                        monthString = "0$monthString"
                        dateStart = "$year-$monthString-$dayString"
                    }

                    dayString.length != 2 -> {
                        dayString = "0$dayString"
                        dateStart = "$year-$monthString-$dayString"
                    }

                    else -> dateStart = "$year-$monthString-$dayString"
                }

                bindingSelectDateJob.startDateInput.setText(date)
            } else {
                conditionCalendar = 0
                date = "$month/$day/$year"

                when {
                    monthString.length != 2 && dayString.length != 2 -> {
                        monthString = "0$monthString"
                        dayString = "0$dayString"
                        dateEnd = "$year-$monthString-$dayString"
                    }

                    monthString.length != 2 -> {
                        monthString = "0$monthString"
                        dateEnd = "$year-$monthString-$dayString"
                    }

                    dayString.length != 2 -> {
                        dayString = "0$dayString"
                        dateEnd = "$year-$monthString-$dayString"
                    }

                    else -> dateEnd = "$year-$monthString-$dayString"
                }

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