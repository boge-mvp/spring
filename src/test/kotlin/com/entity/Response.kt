package com.entity

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response(
	val errno: Int? = null,
	val data: LocalNewsData? = null,
	@SerialName("request_id")
	val requestId: String? = null,
	val timestamp: Int? = null
)

@Serializable
data class Pic(
	val imgUrl: String? = null,
	val time: String? = null,
	val title: String? = null,
	val url: String? = null
)

@Serializable
data class LocalNewsData(
	@SerialName("LocalNews")
	val localNews: LocalNews? = null,
)

@Serializable
data class FirstItem(
	val imgUrl: String? = null,
	val time: String? = null,
	val title: String? = null,
	val url: String? = null
)

@Serializable
data class LocalNews(
	val errno: Int? = null,
	val ad: Ad? = null,
	val data: Data? = null
)

@Serializable
data class Data(
	val name: String? = null,
	@SerialName("cityid")
	val cityId: Int? = null,
	val rows: Rows? = null
)

@Serializable
data class Rows(
	val pic: Pic? = null,
	val first: List<FirstItem?>? = null,
	val second: List<SecondItem?>? = null
)

@Serializable
data class SecondItem(
	val imgUrl: String? = null,
	val time: String? = null,
	val title: String? = null,
	val url: String? = null
)



data class Response2(
	val errno: Int? = null,
	val data: Data2? = null,
	@SerialName("request_id")
	val requestId: String? = null,
	val timestamp: Int? = null
)

data class Data2(
	val ad: Ad? = null
)

@Serializable
data class Ad(
	@Contextual
	val ad: Any? = null
)

