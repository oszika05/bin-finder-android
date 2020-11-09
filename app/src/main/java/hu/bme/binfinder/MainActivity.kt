package hu.bme.binfinder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import hu.bme.binfinder.MapsActivity.Companion.AUTH_TOKEN


class MainActivity : AppCompatActivity() {

    private lateinit var googleSingInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()

        googleSingInClient = GoogleSignIn.getClient(this, gso)

        // 720393882245-pvir40g1g03f0ev74jvjo209cutv8ou5.apps.googleusercontent.com
        // secret: zM2gJFrzkYc0PWY8UHB9gDmC

        // 720393882245-qnqpktn1iqrdoenbhq307bskhvmtkjhl.apps.googleusercontent.com
        // dOEJrr52oXkZBJc3LCot1j3C

        googleSingInClient.silentSignIn().addOnCompleteListener { task ->
            handleSignInResult(task)
        }
    }

    override fun onStart() {
        super.onStart()

//        val account = GoogleSignIn.getLastSignedInAccount(this)

//        updateUI(account)
    }

    private fun signIn() {
        val signInIntent: Intent = googleSingInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("LOGIN", "signInResult:failed code=" + e.statusCode + " ${e.message}")
            updateUI(null)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account == null) {
            return
        }

        Log.d("LOGIN", "login was successfull: ${account.displayName} ${account.idToken}")

        // we can continue to the next fragment
        startActivity(Intent(this, MapsActivity::class.java).apply {
            putExtra(AUTH_TOKEN, account.idToken)
        })
    }

    companion object {
        const val RC_SIGN_IN = 101
    }
}