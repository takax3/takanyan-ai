package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import twitter4j.auth.AccessToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object ReadAccessTokens {
	
	fun read(consumerKey: String, consumerSecret: String, filename: String? = "AccessToken.json"): AccessToken? {
		
		val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
		var accessToken: AccessToken?
		
		File(filename).run {
			if (!exists()) {
				
				accessToken = OAuth().getAccessToken(consumerKey, consumerSecret)
				
				if (accessToken !is AccessToken) return null
				
				try {
					
					FileWriter(this).run {
						write(gson.toJson(JsonObject().apply {
							addProperty("AccessToken", accessToken!!.token)
							addProperty("AccessTokenSecret", accessToken!!.tokenSecret)
						}))
						close()
					}
					
				} catch (e: Exception) {
					e.printStackTrace()
					return null
				}
				
			} else {
				try {
					FileReader(this).run {
						gson.fromJson(this, JsonObject::class.java).run {
							accessToken = AccessToken(get("AccessToken").asString, get("AccessTokenSecret").asString)
						}
						close()
					}
					
				} catch (e: Exception) {
					e.printStackTrace()
					return null
				}
				
			}
		}
		
		return accessToken
		
	}
	
	
}