package com.example.cardcredito

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var cardFront: View
    private lateinit var cardBack: View
    private lateinit var frontBg: ConstraintLayout
    private lateinit var backBg: ConstraintLayout
    private lateinit var imgBandeira: ImageView

    private lateinit var txtNumeroCartao: TextView
    private lateinit var txtNomeTitular: TextView
    private lateinit var txtValidade: TextView
    private lateinit var txtCvv: TextView

    private lateinit var edtNumero: TextInputEditText
    private lateinit var edtNome: TextInputEditText
    private lateinit var edtValidade: TextInputEditText
    private lateinit var edtCvv: TextInputEditText

    private var isBackShowing = false

    private var isFormattingNumber = false
    private var isFormattingValidity = false

    enum class Brand { VISA, MASTERCARD, OTHER }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupFlip()
        setupMasksAndRealtime()
    }

    private fun bindViews() {
        cardFront = findViewById(R.id.cardFront)
        cardBack = findViewById(R.id.cardBack)
        frontBg = findViewById(R.id.frontBg)
        backBg = findViewById(R.id.backBg)
        imgBandeira = findViewById(R.id.imgBandeira)

        txtNumeroCartao = findViewById(R.id.txtNumeroCartao)
        txtNomeTitular = findViewById(R.id.txtNomeTitular)
        txtValidade = findViewById(R.id.txtValidade)
        txtCvv = findViewById(R.id.txtCvv)

        edtNumero = findViewById(R.id.edtNumero)
        edtNome = findViewById(R.id.edtNome)
        edtValidade = findViewById(R.id.edtValidade)
        edtCvv = findViewById(R.id.edtCvv)
    }

    private fun setupFlip() {

        edtCvv.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showBack() else showFront()
        }

        val goFrontListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showFront()
        }
        edtNumero.onFocusChangeListener = goFrontListener
        edtNome.onFocusChangeListener = goFrontListener
        edtValidade.onFocusChangeListener = goFrontListener
    }

    private fun showBack() {
        if (isBackShowing) return
        flip(from = cardFront, to = cardBack)
        isBackShowing = true
    }

    private fun showFront() {
        if (!isBackShowing) return
        flip(from = cardBack, to = cardFront)
        isBackShowing = false
    }

    private fun flip(from: View, to: View) {
        val scale = resources.displayMetrics.density
        from.cameraDistance = 8000 * scale
        to.cameraDistance = 8000 * scale

        val outAnim = ObjectAnimator.ofFloat(from, "rotationY", 0f, 90f).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
        }

        val inAnim = ObjectAnimator.ofFloat(to, "rotationY", -90f, 0f).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
        }

        outAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                from.visibility = View.GONE
                to.visibility = View.VISIBLE
                inAnim.start()
            }
        })

        outAnim.start()
    }

    private fun setupMasksAndRealtime() {

        edtNumero.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingNumber) return
                isFormattingNumber = true

                val digits = (s?.toString() ?: "").filter { it.isDigit() }.take(16)
                val formatted = digits.chunked(4).joinToString(" ")

                if (formatted != s?.toString()) {
                    edtNumero.setText(formatted)
                    edtNumero.setSelection(formatted.length)
                }

                txtNumeroCartao.text = if (formatted.isBlank()) "0000 0000 0000 0000" else formatted

                val brand = detectBrand(digits)
                applyBrand(brand)

                isFormattingNumber = false
            }
        })

        edtNome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = (s?.toString() ?: "").trim()
                txtNomeTitular.text = if (name.isBlank()) "NOME DO TITULAR" else name.uppercase()
            }
        })

        edtValidade.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingValidity) return
                isFormattingValidity = true

                val digits = (s?.toString() ?: "").filter { it.isDigit() }.take(4)
                val formatted = if (digits.length <= 2) digits else digits.substring(0, 2) + "/" + digits.substring(2)

                if (formatted != s?.toString()) {
                    edtValidade.setText(formatted)
                    edtValidade.setSelection(formatted.length)
                }

                txtValidade.text = if (formatted.isBlank()) "MM/AA" else formatted

                isFormattingValidity = false
            }
        })

        edtCvv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val cvv = (s?.toString() ?: "").filter { it.isDigit() }.take(4)
                txtCvv.text = if (cvv.isBlank()) "***" else cvv
            }
        })
    }

    private fun detectBrand(digits: String): Brand {
        if (digits.isEmpty()) return Brand.OTHER

        if (digits.startsWith("4")) return Brand.VISA

        if (digits.length >= 2) {
            val first2 = digits.substring(0, 2).toIntOrNull()
            if (first2 != null && first2 in 51..55) return Brand.MASTERCARD
        }
        if (digits.length >= 4) {
            val first4 = digits.substring(0, 4).toIntOrNull()
            if (first4 != null && first4 in 2221..2720) return Brand.MASTERCARD
        }

        return Brand.OTHER
    }

    private fun applyBrand(brand: Brand) {
        when (brand) {
            Brand.VISA -> {
                imgBandeira.setImageResource(R.drawable.ic_visa)
                frontBg.setBackgroundColor(getColor(R.color.card_visa))
                backBg.setBackgroundColor(getColor(R.color.card_visa))
            }
            Brand.MASTERCARD -> {
                imgBandeira.setImageResource(R.drawable.ic_mastercard)
                frontBg.setBackgroundColor(getColor(R.color.card_master))
                backBg.setBackgroundColor(getColor(R.color.card_master))
            }
            Brand.OTHER -> {
                imgBandeira.setImageResource(R.drawable.ic_card_unknown)
                frontBg.setBackgroundColor(getColor(R.color.card_unknown))
                backBg.setBackgroundColor(getColor(R.color.card_unknown))
            }
        }
    }
}