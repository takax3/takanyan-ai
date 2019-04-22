package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class ReadConsumerKeys {
	
	var consumerKey: String? = null
	var consumerSecret: String? = null
	
	fun read(filename: String = "ConsumerKey.json") : Boolean {
		
		val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
		
		File(filename).run {
			return if (!exists()) {
				try {
					
					FileWriter(this).run {
						write(gson.toJson(JsonObject().apply {
							addProperty("ConsumerKey", "")
							addProperty("ConsumerSecret", "")
						}))
						close()
					}
					println("生成された $filename にCS/CKを書き込んでください")
					false
					
				} catch (e: Exception) {
					e.printStackTrace()
					false
				}
				
			} else {
				try {
					
					println("$filename からCS/CKを読み込みます……")
					FileReader(this).run {
						gson.fromJson(this, JsonObject::class.java).run {
							consumerKey = get("ConsumerKey").asString
							consumerSecret = get("ConsumerSecret").asString
						}
						close()
					}
					println("$filename からCS/CKを読み込みました。")
					true
					
				} catch (e: Exception) {
					e.printStackTrace()
					false
				}
			}
		}
		
	}
	
}