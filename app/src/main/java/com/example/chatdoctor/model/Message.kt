package com.example.chatdoctor.model

data class Message(
    var message: String? = null,
    var senderId: String? = null,
    var timeStamp: Long? = null,
    var messageId: String? = null,
    var imageUrl: String? = null
)