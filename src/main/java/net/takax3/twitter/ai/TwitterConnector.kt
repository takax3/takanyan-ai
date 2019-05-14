package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import java.awt.Desktop
import java.io.*
import java.net.URI

object TwitterConnector {
	
	fun connect(consumerKeyFileName: String = "ConsumerKey.json", accessTokenFileName: String? = "AccessToken.json"): Twitter? {
		
		val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
		
		var consumerKey: String? = null
		var consumerSecret: String? = null
		var accessToken: AccessToken? = null
		
		if (!File(consumerKeyFileName).run {
					if (!exists()) {
						try {
							
							FileWriter(this).run {
								write(gson.toJson(JsonObject().apply {
									addProperty("ConsumerKey", "")
									addProperty("ConsumerSecret", "")
								}))
								close()
							}
							println("生成された $consumerKeyFileName にCS/CKを書き込んでください")
							false
							
						} catch (e: Exception) {
							e.printStackTrace()
							false
						}
						
					} else {
						try {
							
							println("$consumerKeyFileName からCS/CKを読み込みます……")
							FileReader(this).run {
								gson.fromJson(this, JsonObject::class.java).run {
									consumerKey = get("ConsumerKey").asString
									consumerSecret = get("ConsumerSecret").asString
								}
								close()
							}
							println("$consumerKeyFileName からCS/CKを読み込みました。")
							true
							
						} catch (e: Exception) {
							e.printStackTrace()
							false
						}
					}
				}) return null
		
		if (!File(accessTokenFileName).run {
					if (!exists()) {
						
						accessToken = accessTokenGetter(consumerKey!!, consumerSecret!!)
						
						if (accessToken is AccessToken) {
							try {
								
								FileWriter(this).run {
									write(gson.toJson(JsonObject().apply {
										addProperty("AccessToken", accessToken!!.token)
										addProperty("AccessTokenSecret", accessToken!!.tokenSecret)
									}))
									close()
								}
								true
								
							} catch (e: Exception) {
								e.printStackTrace()
								false
							}
						} else {
							false
						}
						
					} else {
						try {
							FileReader(this).run {
								gson.fromJson(this, JsonObject::class.java).run {
									accessToken = AccessToken(get("AccessToken").asString, get("AccessTokenSecret").asString)
								}
								close()
							}
							true
							
						} catch (e: Exception) {
							e.printStackTrace()
							false
						}
						
					}
				}) return null
		
		return TwitterFactory().instance.apply {
			setOAuthConsumer(consumerKey, consumerSecret)
			oAuthAccessToken = accessToken
		}
		
	}
	
	private fun accessTokenGetter(consumerKey: String, consumerSecret: String): AccessToken? {
		
		var accessToken: AccessToken? = null
		
		try {
			println("Oauth認証を行います")
			val br = BufferedReader(InputStreamReader(System.`in`))
			while (null == accessToken) {
				val twitter = TwitterFactory().instance
				twitter.setOAuthConsumer(consumerKey, consumerSecret)
				val requestToken = twitter.oAuthRequestToken
				Desktop.getDesktop().browse(URI(requestToken.authenticationURL))
				print("認証後に表示されるPINコードを入力してください... : ")
				val pin = br.readLine()
				accessToken = try {
					if (pin.isNotEmpty()) {
						twitter.getOAuthAccessToken(requestToken, pin)
					} else {
						twitter.oAuthAccessToken
					}
				} catch (e: Exception) {
					e.printStackTrace()
					null
				}
				
			}
			return accessToken
		} catch (e: Exception) {
			e.printStackTrace()
			return null
		}
		
	}
	
}