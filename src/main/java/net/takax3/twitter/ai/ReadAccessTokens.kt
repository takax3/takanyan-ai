package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import twitter4j.auth.AccessToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object ReadAccessTokens {
	
	fun read(consumerKey: String, consumerSecret: String, filename: String? = "AccessToken"): AccessToken? {
		
		val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
		var accessToken: AccessToken?
		val accessTokenFile = File(filename)
		
		if (!accessTokenFile.exists()) {
			
			accessToken = OAuth().getAccessToken(consumerKey, consumerSecret)
			
			if (accessToken !is AccessToken) {
				return null
			}
			
			try {
				val configAccessToken = Config()
				configAccessToken.accessToken = accessToken.token
				configAccessToken.accessSecret = accessToken.tokenSecret
				
				val fileWriter = FileWriter(accessTokenFile)
				
				fileWriter.write(gson.toJson(configAccessToken))
				
				fileWriter.close()
				
				accessToken = AccessToken(configAccessToken.accessToken, configAccessToken.accessSecret)
			} catch (e: Exception) {
				e.printStackTrace()
				return null
			}
			
		} else {
			try {
				val fileReader = FileReader(accessTokenFile)
				
				val configAccessToken = gson.fromJson(fileReader, Config::class.java)
				fileReader.close()
				
				accessToken = AccessToken(configAccessToken.accessToken, configAccessToken.accessSecret)
			} catch (e: Exception) {
				e.printStackTrace()
				return null
			}
			
		}
		
		return accessToken
	
	}
	
	
	class Config {
		
		var accessToken: String? = null
		var accessSecret: String? = null
		
	}
	
}