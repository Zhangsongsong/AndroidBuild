package com.zasko.imageloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.insets.GradientProtection
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.imageloads.activity.ImageThemePagerActivity
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.databinding.ActivityMainBinding
import com.zasko.imageloads.databinding.ItemMainThemeSelectBinding
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.loadImage
import com.zasko.imageloads.utils.onClick
import com.zasko.imageloads.viewmodel.MainViewModel

class MainActivity : BaseActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }


    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initBindLife(this)
        binding.protectionLayout.setProtections(listOf(GradientProtection(WindowInsetsCompat.Side.TOP, getColor(R.color.teal_700))))
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        binding.fragmentRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = MAdapter().apply {
                setData(
                    arrayListOf(
                        MainThemeSelectInfo(
                            cover = "https://i.xiutaku.com/photo/uploadfile/202505/22/9810543470.jpg",
                            this@MainActivity.getString(R.string.xiuren),
                            dataUseFrom = DataUseFrom.PRIVATE_FILE.value,
                            theme = Constants.THEME_TYPE_XIUREN
                        )
                    )
                )
            }
        }

    }

    inner class MAdapter : RecyclerView.Adapter<MAdapter.MHolder>() {

        private val data = ArrayList<MainThemeSelectInfo>()

        fun setData(list: List<MainThemeSelectInfo>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        inner class MHolder(private val binding: ItemMainThemeSelectBinding) : ViewHolder(binding.root) {
            private var currentInfo: MainThemeSelectInfo? = null

            init {
                binding.titleTv.onClick { _ ->
                    currentInfo?.let { info -> ImageThemePagerActivity.start(context = this@MainActivity, info) }
                }
                binding.useDataFromSwitch.setOnCheckedChangeListener { _, isChecked ->
                    currentInfo?.dataUseFrom = if (isChecked) DataUseFrom.PRIVATE_FILE.value else DataUseFrom.NETWORK.value
                }
            }

            fun bind(info: MainThemeSelectInfo) {
                currentInfo = info
                binding.titleTv.text = info.title
                binding.coverIv.loadImage(info.cover)
                binding.useDataFromSwitch.isChecked = info.dataUseFrom == DataUseFrom.PRIVATE_FILE.value
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MHolder {
            return MHolder(ItemMainThemeSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: MHolder, position: Int) {
            return holder.bind(info = data[position])
        }

    }

}