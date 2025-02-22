package edu.msudenver.cs3013.lab_4
//imports
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // main activity
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.maps_fragment, MapsFragment())
                .commit()
        }
    }
}
