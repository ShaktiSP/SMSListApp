package com.example.smsapp.views

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smsapp.R
import com.example.smsapp.adapter.SmsAdapter
import com.example.smsapp.model.SmsData

class MainActivity : AppCompatActivity() {

    private lateinit var smsAdapter: SmsAdapter
    private var senderIdFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        requestSmsPermissions()
        fetchSms()

        findViewById<EditText>(R.id.senderIdInput).apply {
            addTextChangedListener { text ->
                senderIdFilter = text.toString().trim()
                fetchSms()
            }
        }

        findViewById<Button>(R.id.setSenderIdButton).setOnClickListener {
            val trimmedFilter = senderIdFilter?.trim()
            if (!trimmedFilter.isNullOrEmpty()) {
                Toast.makeText(this, "Filtering for: $trimmedFilter", Toast.LENGTH_SHORT).show()
                fetchSms()
            } else {
                Toast.makeText(this, "Please enter a sender ID to filter", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            addAction("android.provider.Telephony.SMS_SENT")
        }
        registerReceiver(smsReceiver, intentFilter)
    }

    private fun setupRecyclerView() {
        smsAdapter = SmsAdapter()
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = smsAdapter
        }
    }

    private fun requestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            if (permissionsToRequest.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        it
                    )
                }) {
                Toast.makeText(
                    this,
                    "SMS permissions are required to read and send messages.",
                    Toast.LENGTH_LONG
                ).show()
            }
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        }
    }

    private fun fetchSms() {
        val smsList = mutableListOf<SmsData>()
        val uriList = listOf(Telephony.Sms.Inbox.CONTENT_URI, Telephony.Sms.Sent.CONTENT_URI)

        uriList.forEach { uri ->
            contentResolver.query(uri, null, null, null, Telephony.Sms.DATE + " DESC")
                ?.use { cursor ->
                    val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                    val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                    val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

                    while (cursor.moveToNext()) {
                        val body = cursor.getString(bodyIndex)
                        val sender = cursor.getString(addressIndex)
                        val date = cursor.getLong(dateIndex)

                        if (senderIdFilter.isNullOrEmpty() || sender == senderIdFilter) {
                            smsList.add(SmsData(body, sender, date))
                        }
                    }
                }
        }
        smsList.sortByDescending { it.date }
        smsAdapter.updateData(smsList, senderIdFilter)
    }

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                    Telephony.Sms.Intents.getMessagesFromIntent(intent).forEach { smsMessage ->
                        val smsData = SmsData(
                            smsMessage.messageBody,
                            smsMessage.originatingAddress ?: "Unknown",
                            smsMessage.timestampMillis
                        )
                        smsAdapter.addSms(smsData, senderIdFilter)
                        fetchSms()
                    }
                }

                "android.provider.Telephony.SMS_SENT" -> {
                    fetchSms()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            fetchSms()
        } else {
            Toast.makeText(this, "Permission denied. Cannot read/send SMS.", Toast.LENGTH_LONG)
                .show()
        }
    }
}