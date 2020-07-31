package com.zwh.spline

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        mRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            run {
                when (checkedId) {
                    R.id.rb_rgb -> {
                        mSpLine.switchSpLineType(ColorCubiqueSpLineView.ColorSpLineType.RGB)
                    }
                    R.id.rb_red -> {
                        mSpLine.switchSpLineType(ColorCubiqueSpLineView.ColorSpLineType.RED)
                    }
                    R.id.rb_green -> {
                        mSpLine.switchSpLineType(ColorCubiqueSpLineView.ColorSpLineType.GREEN)
                    }
                    R.id.rb_blue -> {
                        mSpLine.switchSpLineType(ColorCubiqueSpLineView.ColorSpLineType.BLUE)
                    }
                }
            }
        }
    }
}