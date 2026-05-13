package com.boge.redis

import org.springframework.data.keyvalue.core.KeyValueOperations
import org.springframework.data.keyvalue.repository.support.SimpleKeyValueRepository
import org.springframework.data.repository.core.EntityInformation
import java.util.*

interface RedisExtendRepository {

}

class RedisExtendRepositoryImpl<T, ID>(metadata: EntityInformation<T, ID>, operations: KeyValueOperations) :
    SimpleKeyValueRepository<T, ID>(metadata, operations) {


    override fun findById(id: ID & Any): Optional<T> {
        return super.findById(id)
    }


}