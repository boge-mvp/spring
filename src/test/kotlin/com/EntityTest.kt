package com

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.keyvalue.annotation.KeySpace
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed
import java.sql.Timestamp

@Serializable
@RedisHash("Test_User")
data class User(

    @Id
    var id: Long? = null,
    @Indexed
    var name: String? = null,
    @Indexed
    var age: Int? = null,
    @Contextual
    @Indexed var date: Timestamp? = null,

    @TimeToLive var timeout: Long? = null

)