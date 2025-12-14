package com.nikhil.fruitcrush
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.nikhil.fruitcrush.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var b: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.startBtn.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        b.infoBtn.setOnClickListener {
            showOptionsDialog()
        }
    }

    private fun showOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_options, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val btnAbout = dialogView.findViewById<Button>(R.id.btnAbout)
        val btnPrivacy = dialogView.findViewById<Button>(R.id.btnPrivacy)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
        
        btnAbout.setOnClickListener {
            dialog.dismiss()
            showCustomDialog(getString(R.string.about_title), getString(R.string.about_message))
        }
        
        btnPrivacy.setOnClickListener {
            dialog.dismiss()
            showCustomDialog(getString(R.string.privacy_title), getString(R.string.privacy_message))
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showCustomDialog(title: String, message: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_info, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Make dialog background transparent so our custom shape shows nicely
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val messageView = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val button = dialogView.findViewById<Button>(R.id.dialogButton)

        titleView.text = title
        messageView.text = Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
        
        button.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
