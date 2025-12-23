package ru.netology.nework.fragment

import android.graphics.Color.RED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentAddLocationBinding
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.extensions.DrawableImageProvider
import ru.netology.nework.extensions.ImageInfo
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_POST
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_WALL
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_SPEAKERS_USER
import ru.netology.nework.fragment.UserFragment.Companion.status
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.PostViewModel
import ru.netology.nework.viewmodel.EventViewModel
import ru.netology.nework.viewmodel.MyWallViewModel
import kotlin.getValue

class AddLocationFragment : Fragment() {
    companion object {
        const val POST = "post"
        const val WALL = "wall"
        const val EVENT = "event"
        var statusFragment = ""
        var Bundle.statusAddLocationFragment by StringArg
    }

    private lateinit var yandexMap: Map
    private lateinit var mapKit: MapKit
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var binding: FragmentAddLocationBinding
    private val smoothAnimation = Animation(Animation.Type.SMOOTH, 3F)
    private val listPoint = mutableListOf<Coordinates>()

    private val placemarkTapListener = MapObjectTapListener { mapObject, point ->
        deleteMarker(mapObject, point)
        true
    }

    private val inputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            val target = Point(point.latitude, point.longitude)

            addMarker(map, target)
            moveToMarker(map, target)
        }

        override fun onMapLongTap(map: Map, point: Point) = Unit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddLocationBinding.inflate(layoutInflater, container, false)

        val viewModelPost: PostViewModel by activityViewModels()
        val viewModelMyWall: MyWallViewModel by activityViewModels()
        val viewModelEvent: EventViewModel by activityViewModels()

        arguments?.statusAddLocationFragment?.let {
            statusFragment = it
            arguments?.statusAddLocationFragment = null
        }

        with(binding) {
            back.setOnClickListener {
                findNavController().navigateUp()
            }

            save.setOnClickListener {
                when (status) {
                    POST -> {
                        Toast.makeText(requireContext(), R.string.coordinates_added, Toast.LENGTH_SHORT).show()
                        viewModelPost.addLocation(listPoint.first())
                    }

                    WALL -> {
                        Toast.makeText(requireContext(), R.string.coordinates_added, Toast.LENGTH_SHORT).show()
                        viewModelMyWall.addLocation(listPoint.first())
                    }

                    EVENT -> {
                        Toast.makeText(requireContext(), R.string.coordinates_added, Toast.LENGTH_SHORT).show()
                        viewModelEvent.addLocation(listPoint.first())
                    }
                }

                findNavController().navigateUp()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapView = binding.map
        val mapWindow = mapView.mapWindow

        yandexMap = mapWindow.map
        mapKit = MapKitFactory.getInstance()
        placemarkMapObject = yandexMap.mapObjects.addPlacemark()
        yandexMap.addInputListener(inputListener)

        subscribeToLifecycle(mapView)
    }

    private fun moveToMarker(yandexMap: Map, target: Point) {
        val currentPosition = yandexMap.cameraPosition
        yandexMap.move(
            CameraPosition(
                target, 15F, currentPosition.azimuth, currentPosition.tilt,
            ),
            smoothAnimation,
            null,
        )
    }

    private fun addMarker(yandexMap: Map, target: Point) {
        val imageProvider =
            DrawableImageProvider(requireContext(), ImageInfo(R.drawable.ic_location_pin_48, RED))

        listPoint.add(Coordinates(target.latitude, target.longitude))

        placemarkMapObject = yandexMap.mapObjects.addPlacemark {
            it.setIcon(imageProvider)
            it.geometry = target
            it.setText(context?.getString(R.string.place_work) ?: "")
            it.addTapListener(placemarkTapListener)
//            it.userData = context?.getString(R.string.place_work) ?: ""
        }
    }

    private fun deleteMarker(mapObject: MapObject, point: Point) {
        //TODO(Проверить работу функции remove)
//        listPoint = listPoint.filter { it.latitude != point.latitude && it.longitude != point.longitude }
        listPoint.remove(Coordinates(point.latitude, point.longitude))
        yandexMap.mapObjects.remove(mapObject)
    }

    private fun subscribeToLifecycle(mapView: MapView) {
        viewLifecycleOwner.lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            mapKit.onStart()
                            mapView.onStart()
                        }

                        Lifecycle.Event.ON_STOP -> {
                            mapView.onStop()
                            mapKit.onStop()
                        }

                        Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)

                        else -> Unit
                    }
                }
            }
        )
    }
}