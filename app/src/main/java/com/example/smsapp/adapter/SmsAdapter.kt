package com.example.smsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smsapp.R
import com.example.smsapp.model.SmsData
import java.text.DateFormat
import java.util.Date

class SmsAdapter(private val smsList: MutableList<SmsData> = mutableListOf()) :
    RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {

    inner class SmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bodyView: TextView = itemView.findViewById(R.id.smsBody)
        private val senderView: TextView = itemView.findViewById(R.id.smsSender)
        private val dateView: TextView = itemView.findViewById(R.id.smsDate)

        fun bind(sms: SmsData) {
            bodyView.text = sms.body
            senderView.text = sms.sender
            dateView.text = DateFormat.getDateTimeInstance().format(Date(sms.date))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SmsViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.sms_item, parent, false)
    )

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) =
        holder.bind(smsList[position])

    override fun getItemCount() = smsList.size

    fun updateData(newList: List<SmsData>, senderIdFilter: String?) {
        smsList.apply {
            clear()
            if (senderIdFilter.isNullOrEmpty()) {
                addAll(newList) // Show all messages if no filter
            } else {
                addAll(newList.filter { it.sender == senderIdFilter }) // Filter by senderId
            }
        }
        notifyDataSetChanged()
    }

    fun addSms(sms: SmsData, senderIdFilter: String?) {
        if (senderIdFilter.isNullOrEmpty() || sms.sender == senderIdFilter) {
            smsList.add(0, sms)
            notifyItemInserted(0)
        }
    }
}





